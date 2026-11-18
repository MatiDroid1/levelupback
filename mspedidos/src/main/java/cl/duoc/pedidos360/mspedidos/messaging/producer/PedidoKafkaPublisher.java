package cl.duoc.pedidos360.mspedidos.messaging.producer;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.mspedidos.model.Pedido;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PedidoKafkaPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Value("${app.kafka.topic.pedidos}")
    private String topic;

    public PedidoKafkaPublisher(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        publicar(
                pedido,
                "PEDIDO_CREADO",
                null,
                pedido.getUsuario(),
                pedido.getFecha()
        );
    }

    public void publicarCambioEstado(
            Pedido pedido,
            String estadoAnterior,
            String actor) {

        publicar(
                pedido,
                "PEDIDO_ESTADO_CAMBIADO",
                estadoAnterior,
                actor,
                LocalDateTime.now()
        );
    }

    private void publicar(
            Pedido pedido,
            String tipo,
            String estadoAnterior,
            String actor,
            LocalDateTime fechaEvento) {

        try {
            List<Map<String, Object>> items =
                    pedido.getDetalles().stream()
                            .map(detalle -> {
                                Map<String, Object> item =
                                        new LinkedHashMap<>();

                                item.put(
                                        "productoId",
                                        detalle.getProductoId()
                                );
                                item.put(
                                        "cantidad",
                                        detalle.getCantidad()
                                );

                                return item;
                            })
                            .toList();

            Map<String, Object> evento = new LinkedHashMap<>();
            evento.put("eventId", UUID.randomUUID().toString());
            evento.put("tipo", tipo);
            evento.put("pedidoId", pedido.getId());

            // Propietario del pedido y autor de la accion.
            evento.put("usuario", pedido.getUsuario());
            evento.put("actor", actor);

            evento.put("estadoAnterior", estadoAnterior);
            evento.put("estado", pedido.getEstado());
            evento.put("total", pedido.getTotal());

            // fecha conserva su significado: creacion del pedido.
            evento.put("fecha", pedido.getFecha().toString());
            evento.put("fechaEvento", fechaEvento.toString());
            evento.put("items", items);

            String payload = mapper.writeValueAsString(evento);

            kafka.send(
                    topic,
                    String.valueOf(pedido.getId()),
                    payload
            ).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error(
                            "Kafka fallo | pedido={} | tipo={}",
                            pedido.getId(),
                            tipo,
                            error
                    );
                } else {
                    log.info(
                            "Kafka OK | pedido={} | tipo={} "
                                    + "| particion={} | offset={}",
                            pedido.getId(),
                            tipo,
                            resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset()
                    );
                }
            });

        } catch (Exception e) {
            log.error(
                    "No se pudo preparar/publicar evento "
                            + "| pedido={} | tipo={}",
                    pedido.getId(),
                    tipo,
                    e
            );
        }
    }
}