package cl.duoc.pedidos360.mspedidos.messaging.consumer.notification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

@Component
public class NotificacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionConsumer.class);

    @RabbitListener(queues = "${app.rabbit.notificaciones.queue}")
    public void recibir(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            procesar(body);
            channel.basicAck(tag, false);
            log.info("[NOTIFICACION] Procesado y confirmado (ACK): {}", body);
        } catch (IllegalArgumentException e) {
            log.error("[NOTIFICACION] Error no recuperable, enviado a DLQ: {}", e.getMessage());
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            if (message.getMessageProperties().isRedelivered()) {
                log.error("[NOTIFICACION] Fallo tras reintento, enviado a DLQ: {}", e.getMessage());
                channel.basicNack(tag, false, false);
            } else {
                log.warn("[NOTIFICACION] Error recuperable, se reintenta: {}", e.getMessage());
                channel.basicNack(tag, false, true);
            }
        }
    }

    private void procesar(String body) {
        if (body.contains("falla-dlq")) {
            throw new IllegalArgumentException("Mensaje invalido para notificacion");
        }
        if (body.contains("reintento")) {
            throw new IllegalStateException("Servicio de notificaciones no disponible");
        }
        log.info("[NOTIFICACION] Enviando aviso al cliente...");
    }
}