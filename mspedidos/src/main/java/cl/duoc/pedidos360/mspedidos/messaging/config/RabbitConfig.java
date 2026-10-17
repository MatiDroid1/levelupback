package cl.duoc.pedidos360.mspedidos.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbit.exchange.topic}") private String topicExchange;
    @Value("${app.rabbit.exchange.direct}") private String directExchange;
    @Value("${app.rabbit.exchange.dlx}") private String dlxExchange;

    @Value("${app.rabbit.notificaciones.queue}") private String notifQueue;
    @Value("${app.rabbit.notificaciones.dlq}") private String notifDlq;
    @Value("${app.rabbit.notificaciones.routing-key}") private String notifKey;
    @Value("${app.rabbit.notificaciones.dlq-routing-key}") private String notifDlqKey;

    @Value("${app.rabbit.tickets.queue}") private String ticketQueue;
    @Value("${app.rabbit.tickets.dlq}") private String ticketDlq;
    @Value("${app.rabbit.tickets.routing-key}") private String ticketKey;
    @Value("${app.rabbit.tickets.dlq-routing-key}") private String ticketDlqKey;

    @Value("${app.rabbit.documentos.queue}") private String docQueue;
    @Value("${app.rabbit.documentos.dlq}") private String docDlq;
    @Value("${app.rabbit.documentos.routing-key}") private String docKey;
    @Value("${app.rabbit.documentos.dlq-routing-key}") private String docDlqKey;

    // ---------- Exchanges ----------
    @Bean public TopicExchange pedidosTopicExchange() { return new TopicExchange(topicExchange, true, false); }
    @Bean public DirectExchange pedidosDirectExchange() { return new DirectExchange(directExchange, true, false); }
    @Bean public DirectExchange pedidosDlxExchange() { return new DirectExchange(dlxExchange, true, false); }

    // ---------- Notificaciones ----------
    @Bean public Queue notificacionesQueue() {
        return QueueBuilder.durable(notifQueue)
                .deadLetterExchange(dlxExchange)
                .deadLetterRoutingKey(notifDlqKey).build();
    }
    @Bean public Queue notificacionesDlq() { return QueueBuilder.durable(notifDlq).build(); }
    @Bean public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue()).to(pedidosTopicExchange()).with(notifKey);
    }
    @Bean public Binding notificacionesDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq()).to(pedidosDlxExchange()).with(notifDlqKey);
    }

    // ---------- Tickets ----------
    @Bean public Queue ticketsQueue() {
        return QueueBuilder.durable(ticketQueue)
                .deadLetterExchange(dlxExchange)
                .deadLetterRoutingKey(ticketDlqKey).build();
    }
    @Bean public Queue ticketsDlq() { return QueueBuilder.durable(ticketDlq).build(); }
    @Bean public Binding ticketsBinding() {
        return BindingBuilder.bind(ticketsQueue()).to(pedidosDirectExchange()).with(ticketKey);
    }
    @Bean public Binding ticketsDlqBinding() {
        return BindingBuilder.bind(ticketsDlq()).to(pedidosDlxExchange()).with(ticketDlqKey);
    }

    // ---------- Documentos ----------
    @Bean public Queue documentosQueue() {
        return QueueBuilder.durable(docQueue)
                .deadLetterExchange(dlxExchange)
                .deadLetterRoutingKey(docDlqKey).build();
    }
    @Bean public Queue documentosDlq() { return QueueBuilder.durable(docDlq).build(); }
    @Bean public Binding documentosBinding() {
        return BindingBuilder.bind(documentosQueue()).to(pedidosDirectExchange()).with(docKey);
    }
    @Bean public Binding documentosDlqBinding() {
        return BindingBuilder.bind(documentosDlq()).to(pedidosDlxExchange()).with(docDlqKey);
    }
}