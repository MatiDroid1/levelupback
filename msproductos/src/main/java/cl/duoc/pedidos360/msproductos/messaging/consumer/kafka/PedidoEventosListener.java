package cl.duoc.pedidos360.msproductos.messaging.consumer.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PedidoEventosListener {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoEventosListener.class);

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Value("${app.fraude.cantidad-maxima:100}")
    private int cantidadMaxima;

    @Value("${app.fraude.total-maximo:5000000}")
    private long totalMaximo;

    @KafkaListener(topics = "${app.kafka.topic.pedidos}")
    public void onPedido(String mensaje) {
        try {
            JsonNode evento = mapper.readTree(mensaje);

            String tipo = evento.path("tipo").asText();

            if (!"PEDIDO_CREADO".equals(tipo)) {
                log.debug("Evento ignorado por detector de fraude: {}", tipo);
                return;
            }

            long pedidoId = evento.path("pedidoId").asLong();
            JsonNode items = evento.path("items");

            if (pedidoId <= 0 || !items.isArray() || items.size() == 0) {
                throw new IllegalArgumentException(
                        "El evento debe contener pedidoId válido e items");
            }

            log.info("Evento PEDIDO_CREADO recibido: pedido {}", pedidoId);

            long total = evento.path("total").asLong();
            StringBuilder motivo = new StringBuilder();

            if (total >= totalMaximo) {
                motivo.append("total ")
                        .append(total)
                        .append(" >= ")
                        .append(totalMaximo)
                        .append("; ");
            }

            for (JsonNode item : items) {
                int cantidad = item.path("cantidad").asInt();

                if (cantidad >= cantidadMaxima) {
                    motivo.append("producto ")
                            .append(item.path("productoId").asText())
                            .append(" cantidad ")
                            .append(cantidad)
                            .append(" >= ")
                            .append(cantidadMaxima)
                            .append("; ");
                }
            }

            if (motivo.length() > 0) {
                log.warn(
                        "ALERTA DE FRAUDE: pedido {} sospechoso -> {}",
                        pedidoId,
                        motivo);
            }

        } catch (Exception e) {
            log.error(
                    "Evento inválido descartado: {} -> {}",
                    mensaje,
                    e.getMessage());
        }
    }
}