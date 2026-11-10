package cl.duoc.pedidos360.mspedidos.messaging.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.mspedidos.model.DetallePedido;
import cl.duoc.pedidos360.mspedidos.model.Pedido;

@Component
public class PedidoKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafka;

    @Value("${app.kafka.topic.pedidos}") private String topic;

    public PedidoKafkaPublisher(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        try {
            StringBuilder items = new StringBuilder();
            for (DetallePedido d : pedido.getDetalles()) {
                if (items.length() > 0) items.append(",");
                items.append(String.format("{\"productoId\":%d,\"cantidad\":%d}",
                        d.getProductoId(), d.getCantidad()));
            }
            String payload = String.format(
                "{\"eventId\":\"%s\",\"tipo\":\"PEDIDO_CREADO\",\"pedidoId\":%d,"
                + "\"total\":%d,\"fecha\":\"%s\",\"items\":[%s]}",
                java.util.UUID.randomUUID(), pedido.getId(), pedido.getTotal(),
                pedido.getFecha(), items);

            kafka.send(topic, String.valueOf(pedido.getId()), payload)
                 .whenComplete((r, ex) -> {
                     if (ex != null) log.error("Kafka fallo pedido {}: {}", pedido.getId(), ex.getMessage());
                     else log.info("Kafka OK pedido {} -> particion {} offset {}",
                             pedido.getId(), r.getRecordMetadata().partition(), r.getRecordMetadata().offset());
                 });
        } catch (Exception e) {
            log.error("No se pudo publicar en Kafka el pedido {}: {}", pedido.getId(), e.getMessage());
        }
    }
}