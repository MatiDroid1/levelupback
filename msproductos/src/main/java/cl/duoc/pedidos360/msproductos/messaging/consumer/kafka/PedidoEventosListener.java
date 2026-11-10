package cl.duoc.pedidos360.msproductos.messaging.consumer.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PedidoEventosListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventosListener.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final KafkaTemplate<String, String> kafka;

    @Value("${app.kafka.topic.stock}") private String topicStock;

    public PedidoEventosListener(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @KafkaListener(topics = "${app.kafka.topic.pedidos}")
    public void onPedido(String mensaje) {
        try {
            JsonNode evento = mapper.readTree(mensaje);
            long pedidoId = evento.get("pedidoId").asLong();
            log.info("Evento recibido: pedido {}", pedidoId);

            for (JsonNode item : evento.get("items")) {
                String productoId = item.get("productoId").asText();
                String mov = String.format(
                    "{\"productoId\":%s,\"cantidad\":%d,\"tipo\":\"SALIDA\",\"pedidoId\":%d}",
                    productoId, item.get("cantidad").asInt(), pedidoId);
                kafka.send(topicStock, productoId, mov);
            }
        } catch (Exception e) {
            log.error("Evento invalido descartado: {} -> {}", mensaje, e.getMessage());
        }
    }
}