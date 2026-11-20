package cl.duoc.pedidos360.mspedidos.messaging.consumer.notification;

import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.mspedidos.service.NotificacionService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class NotificacionKafkaConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    NotificacionKafkaConsumer.class
            );

    private static final Set<String> ESTADOS_ADMITIDOS =
            Set.of(
                    "CREADO",
                    "ACEPTADO",
                    "EN_PREPARACIÓN",
                    "DESPACHADO",
                    "ENTREGADO",
                    "CANCELADO"
            );

    private final NotificacionService service;

    private final JsonMapper mapper =
            JsonMapper.builder().build();

    public NotificacionKafkaConsumer(
            NotificacionService service) {

        this.service = service;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.pedidos}",
            groupId = "${app.kafka.notificaciones.group-id:pedidos-notificaciones-v1}",
            properties = {
                    "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "auto.offset.reset=latest",
                    "enable.auto.commit=false"
            }
    )
    public void recibir(String payload) {

        String eventoId;
        String usuario;
        String estado;
        long pedidoId;
        LocalDateTime fecha;

        try {
            JsonNode evento = mapper.readTree(payload);

            String tipo = texto(evento, "tipo");

            if (!"PEDIDO_CREADO".equals(tipo)
                    && !"PEDIDO_ESTADO_CAMBIADO".equals(tipo)) {

                return;
            }

            eventoId = texto(evento, "eventId");
            usuario = texto(evento, "usuario");
            estado = texto(evento, "estado");

            JsonNode id = evento.get("pedidoId");
            pedidoId = id == null ? 0 : id.asLong();

            if (eventoId.isBlank()
                    || eventoId.length() > 100
                    || usuario.isBlank()
                    || usuario.length() > 320
                    || pedidoId <= 0
                    || !ESTADOS_ADMITIDOS.contains(estado)) {

                log.warn(
                        "Notificación omitida: evento incompleto "
                                + "o estado no admitido"
                );

                return;
            }

            String fechaTexto = texto(
                    evento,
                    "fechaEvento"
            );

            fecha = fechaTexto.isBlank()
                    ? LocalDateTime.now()
                    : LocalDateTime.parse(fechaTexto);

        } catch (Exception e) {

            log.warn(
                    "Notificación omitida: formato inválido ({})",
                    e.getClass().getSimpleName()
            );

            return;
        }

        try {
            service.registrar(
                    eventoId,
                    usuario,
                    pedidoId,
                    estado,
                    fecha
            );

        } catch (DataIntegrityViolationException e) {

            // Puede ocurrir si el mismo evento ya fue guardado
            // por otro procesamiento concurrente.
            if (!service.eventoProcesado(eventoId)) {
                throw e;
            }
        }

        log.info(
                "Notificación procesada | evento={} "
                        + "| pedido={} | estado={}",
                eventoId,
                pedidoId,
                estado
        );
    }

    private String texto(
            JsonNode nodo,
            String campo) {

        JsonNode valor = nodo.get(campo);

        return valor == null || valor.isNull()
                ? ""
                : valor.asText().trim();
    }
}