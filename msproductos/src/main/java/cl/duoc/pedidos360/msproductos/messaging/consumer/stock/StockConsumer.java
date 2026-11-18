package cl.duoc.pedidos360.msproductos.messaging.consumer.stock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.msproductos.service.StockService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StockConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(StockConsumer.class);

    private final StockService stockService;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper mapper = new JsonMapper();

    @Value("${app.kafka.topic.stock}")
    private String topicStock;

    public StockConsumer(
            StockService stockService,
            KafkaTemplate<String, String> kafka) {

        this.stockService = stockService;
        this.kafka = kafka;
    }

    @RabbitListener(queues = "${app.rabbit.stock.queue}")
    public void recibir(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();

        String body = new String(
                message.getBody(),
                StandardCharsets.UTF_8);

        try {
            JsonNode json = mapper.readTree(body);
            long pedidoId = json.path("pedidoId").longValue();

            if (pedidoId <= 0) {
                throw new IllegalArgumentException(
                        "El identificador del pedido es inválido");
            }

            JsonNode items = json.path("items");

            if (!items.isArray() || items.size() == 0) {
                throw new IllegalArgumentException(
                        "El mensaje debe contener un arreglo de items no vacío");
            }

            Map<Long, Integer> cantidades = new LinkedHashMap<>();

            for (JsonNode item : items) {
                long productoId = item.path("productoId").longValue();
                int cantidad = item.path("cantidad").intValue();

                if (productoId <= 0 || cantidad <= 0) {
                    throw new IllegalArgumentException(
                            "Cada item debe tener productoId y cantidad positivos");
                }

                cantidades.merge(
                        productoId,
                        cantidad,
                        Math::addExact);
            }

            stockService.descontarPedido(cantidades);

            log.info(
                    "Stock descontado para el pedido {}: {}",
                    pedidoId,
                    cantidades);

            publicarMovimientosStock(pedidoId, cantidades);

        } catch (IllegalArgumentException
                | IllegalStateException
                | JacksonException
                | ArithmeticException e) {

            log.error(
                    "Mensaje de stock rechazado sin reencolar: {} | cuerpo={}",
                    e.getMessage(),
                    body);

            channel.basicNack(tag, false, false);
            return;

        } catch (Exception e) {
            if (message.getMessageProperties().isRedelivered()) {
                log.error(
                        "Fallo en mensaje reentregado; se rechaza "
                                + "sin reencolar: {} | cuerpo={}",
                        e.getMessage(),
                        body);

                channel.basicNack(tag, false, false);
            } else {
                log.warn(
                        "Fallo de procesamiento; se solicita reentrega: {}",
                        e.getMessage());

                channel.basicNack(tag, false, true);
            }

            return;
        }

        channel.basicAck(tag, false);
    }

    private void publicarMovimientosStock(
            long pedidoId,
            Map<Long, Integer> cantidades) {

        cantidades.forEach((productoId, cantidad) -> {
            String movimiento = String.format(
                    "{\"productoId\":%d,\"cantidad\":%d,"
                            + "\"tipo\":\"SALIDA\",\"pedidoId\":%d}",
                    productoId,
                    cantidad,
                    pedidoId);

            try {
                kafka.send(
                        topicStock,
                        String.valueOf(productoId),
                        movimiento
                ).whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.error(
                                "Stock ya descontado, pero falló la publicación "
                                        + "Kafka: pedido {}, producto {}",
                                pedidoId,
                                productoId,
                                error);
                    } else {
                        log.info(
                                "Movimiento Kafka OK: pedido {}, producto {}, "
                                        + "cantidad {}, partición {}, offset {}",
                                pedidoId,
                                productoId,
                                cantidad,
                                resultado.getRecordMetadata().partition(),
                                resultado.getRecordMetadata().offset());
                    }
                });

            } catch (Exception e) {
                log.error(
                        "Stock ya descontado, pero no se pudo enviar "
                                + "el movimiento Kafka: pedido {}, producto {}",
                        pedidoId,
                        productoId,
                        e);
            }
        });
    }
}