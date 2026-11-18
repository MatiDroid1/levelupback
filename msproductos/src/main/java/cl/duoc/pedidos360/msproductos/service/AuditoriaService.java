package cl.duoc.pedidos360.msproductos.service;

import java.time.LocalDateTime;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.msproductos.model.EventoAuditoria;
import cl.duoc.pedidos360.msproductos.repository.EventoAuditoriaRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AuditoriaService {

    private final EventoAuditoriaRepository repo;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public AuditoriaService(EventoAuditoriaRepository repo) {
        this.repo = repo;
    }

    public boolean registrar(ConsumerRecord<String, String> record) {
        JsonNode evento = mapper.readTree(record.value());

        if (evento == null || !evento.isObject()) {
            throw new IllegalArgumentException(
                    "El evento debe ser un objeto JSON"
            );
        }

        String tipo = texto(evento, "tipo");

        if (!"PEDIDO_CREADO".equals(tipo)
                && !"PEDIDO_ESTADO_CAMBIADO".equals(tipo)) {
            return false;
        }

        String eventId = textoObligatorio(evento, "eventId");

        if (repo.existsByEventId(eventId)) {
            return false;
        }

        JsonNode pedidoId = evento.path("pedidoId");

        if (!pedidoId.isIntegralNumber()
                || !pedidoId.canConvertToLong()
                || pedidoId.asLong() <= 0) {
            throw new IllegalArgumentException(
                    "pedidoId debe ser un entero positivo"
            );
        }

        LocalDateTime fechaPedido = LocalDateTime.parse(
                textoObligatorio(evento, "fecha")
        );

        String fechaEventoTexto = texto(evento, "fechaEvento");

        LocalDateTime fechaEvento;

        if (fechaEventoTexto != null) {
            fechaEvento = LocalDateTime.parse(fechaEventoTexto);
        } else if ("PEDIDO_CREADO".equals(tipo)) {
            // Compatibilidad con eventos antiguos de creacion.
            fechaEvento = fechaPedido;
        } else {
            throw new IllegalArgumentException(
                    "El cambio de estado requiere fechaEvento"
            );
        }

        String estado = texto(evento, "estado");

        if (estado == null && "PEDIDO_CREADO".equals(tipo)) {
            estado = "CREADO";
        }

        String estadoAnterior = texto(evento, "estadoAnterior");

        if ("PEDIDO_ESTADO_CAMBIADO".equals(tipo)
                && (estado == null || estadoAnterior == null)) {
            throw new IllegalArgumentException(
                    "El cambio requiere estado y estadoAnterior"
            );
        }

        JsonNode total = evento.path("total");

        if (!total.isIntegralNumber()
                || !total.canConvertToLong()
                || total.asLong() < 0) {
            throw new IllegalArgumentException(
                    "El total debe ser un entero no negativo"
            );
        }

        EventoAuditoria registro = new EventoAuditoria();
        registro.setEventId(eventId);
        registro.setTipo(tipo);
        registro.setPedidoId(pedidoId.asLong());
        registro.setUsuario(texto(evento, "usuario"));
        registro.setActor(texto(evento, "actor"));
        registro.setEstadoAnterior(estadoAnterior);
        registro.setEstado(estado);
        registro.setTotal(total.asLong());
        registro.setFechaPedido(fechaPedido);
        registro.setFechaEvento(fechaEvento);
        registro.setFechaRegistro(LocalDateTime.now());
        registro.setTopico(record.topic());
        registro.setParticion(record.partition());
        registro.setOffsetKafka(record.offset());
        registro.setPayloadOriginal(record.value());

        try {
            repo.saveAndFlush(registro);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Puede haber llegado otra entrega del mismo evento.
            if (repo.existsByEventId(eventId)) {
                return false;
            }

            throw e;
        }
    }

    private String texto(JsonNode evento, String campo) {
        JsonNode valor = evento.path(campo);

        if (!valor.isTextual()) {
            return null;
        }

        String resultado = valor.asText().trim();

        return resultado.isEmpty() ? null : resultado;
    }

    private String textoObligatorio(JsonNode evento, String campo) {
        String valor = texto(evento, campo);

        if (valor == null) {
            throw new IllegalArgumentException(
                    "Falta el campo obligatorio: " + campo
            );
        }

        return valor;
    }
}