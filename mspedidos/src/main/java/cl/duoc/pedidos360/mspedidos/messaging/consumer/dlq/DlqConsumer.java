package cl.duoc.pedidos360.mspedidos.messaging.consumer.dlq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

@Component
public class DlqConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(DlqConsumer.class);

    @RabbitListener(
            queues = {
                    "${app.rabbit.notificaciones.dlq}",
                    "${app.rabbit.tickets.dlq}",
                    "${app.rabbit.documentos.dlq}"
            },
            ackMode = "MANUAL")
    public void recibir(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();

        String cola =
                message.getMessageProperties().getConsumerQueue();

        String cuerpo = new String(
                message.getBody(),
                StandardCharsets.UTF_8);

        Object historial =
                message.getMessageProperties()
                        .getHeaders()
                        .get("x-death");

        log.error(
                "[DLQ] Mensaje recibido | cola={} | x-death={} | cuerpo={}",
                cola,
                historial,
                cuerpo);

        channel.basicAck(tag, false);

        log.info(
                "[DLQ] Mensaje registrado y confirmado | cola={}",
                cola);
    }
}