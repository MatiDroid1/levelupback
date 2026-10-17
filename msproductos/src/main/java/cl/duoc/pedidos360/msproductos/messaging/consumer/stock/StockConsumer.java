package cl.duoc.pedidos360.msproductos.messaging.consumer.stock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.msproductos.service.StockService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StockConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockConsumer.class);

    private final StockService stockService;
    private final JsonMapper mapper = new JsonMapper();

    public StockConsumer(StockService stockService) {
        this.stockService = stockService;
    }

    @RabbitListener(queues = "${app.rabbit.stock.queue}")
    public void recibir(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            JsonNode json = mapper.readTree(body);
            long pedidoId = json.path("pedidoId").longValue();

            Map<Long, Integer> cantidades = new LinkedHashMap<>();
            for (JsonNode item : json.path("items")) {
                cantidades.merge(item.path("productoId").longValue(),
                        item.path("cantidad").intValue(), Integer::sum);
            }
            if (cantidades.isEmpty()) {
                throw new IllegalArgumentException("El mensaje no trae items");
            }

            stockService.descontarPedido(cantidades);
            channel.basicAck(tag, false);
            log.info("Stock descontado para el pedido {}: {}", pedidoId, cantidades);

        } catch (IllegalArgumentException | IllegalStateException | JacksonException e) {
            // Error no recuperable: reintentar no sirve. Va directo a la DLQ.
            log.error("Mensaje de stock rechazado, enviado a la DLQ: {} | cuerpo={}", e.getMessage(), body);
            channel.basicNack(tag, false, false);

        } catch (Exception e) {
            // Posible error transitorio (por ejemplo la base de datos): un reintento y luego DLQ.
            if (message.getMessageProperties().isRedelivered()) {
                log.error("Fallo repetido, enviado a la DLQ: {} | cuerpo={}", e.getMessage(), body);
                channel.basicNack(tag, false, false);
            } else {
                log.warn("Fallo transitorio, se reintenta una vez: {}", e.getMessage());
                channel.basicNack(tag, false, true);
            }
        }
    }
}