package cl.duoc.pedidos360.mspedidos.messaging.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.mspedidos.model.DetallePedido;
import cl.duoc.pedidos360.mspedidos.model.Pedido;

@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbit.exchange.topic}") private String topicExchange;
    @Value("${app.rabbit.exchange.direct}") private String directExchange;
    @Value("${app.rabbit.notificaciones.routing-key}") private String notifKey;
    @Value("${app.rabbit.tickets.routing-key}") private String ticketKey;
    @Value("${app.rabbit.documentos.routing-key}") private String docKey;
    @Value("${app.rabbit.stock.routing-key}") private String stockKey;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        try {
            String usuario = String.valueOf(pedido.getUsuario()).replace("\"", "\\\"");
            String payload = String.format(
                "{\"pedidoId\":%d,\"usuario\":\"%s\",\"total\":%d,\"fecha\":\"%s\"}",
                pedido.getId(), usuario, pedido.getTotal(), pedido.getFecha());

            rabbitTemplate.convertAndSend(topicExchange, notifKey, payload);
            rabbitTemplate.convertAndSend(directExchange, ticketKey, payload);
            rabbitTemplate.convertAndSend(directExchange, docKey, payload);

            rabbitTemplate.convertAndSend(topicExchange, stockKey, construirPayloadStock(pedido));

            log.info("Eventos publicados para el pedido {}", pedido.getId());
        } catch (Exception e) {
            log.error("No se pudo publicar el pedido {} en RabbitMQ: {}", pedido.getId(), e.getMessage());
        }
    }

    private String construirPayloadStock(Pedido pedido) {
        StringBuilder items = new StringBuilder();
        if (pedido.getDetalles() != null) {
            for (DetallePedido d : pedido.getDetalles()) {
                if (items.length() > 0) items.append(",");
                items.append(String.format("{\"productoId\":%d,\"cantidad\":%d}",
                        d.getProductoId(), d.getCantidad()));
            }
        }
        return String.format("{\"pedidoId\":%d,\"items\":[%s]}", pedido.getId(), items);
    }
}