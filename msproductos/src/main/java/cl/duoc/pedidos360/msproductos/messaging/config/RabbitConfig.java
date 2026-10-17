package cl.duoc.pedidos360.msproductos.messaging.config;

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
    @Value("${app.rabbit.exchange.dlx}") private String dlxExchange;

    @Value("${app.rabbit.stock.queue}") private String stockQueue;
    @Value("${app.rabbit.stock.dlq}") private String stockDlq;
    @Value("${app.rabbit.stock.routing-key}") private String stockKey;
    @Value("${app.rabbit.stock.dlq-routing-key}") private String stockDlqKey;

    // ---------- Exchanges ----------
    @Bean public TopicExchange pedidosTopicExchange() { return new TopicExchange(topicExchange, true, false); }
    @Bean public DirectExchange pedidosDlxExchange() { return new DirectExchange(dlxExchange, true, false); }

    // ---------- Stock ----------
    @Bean public Queue stockQueue() {
        return QueueBuilder.durable(stockQueue)
                .deadLetterExchange(dlxExchange)
                .deadLetterRoutingKey(stockDlqKey).build();
    }
    @Bean public Queue stockDlq() { return QueueBuilder.durable(stockDlq).build(); }
    @Bean public Binding stockBinding() {
        return BindingBuilder.bind(stockQueue()).to(pedidosTopicExchange()).with(stockKey);
    }
    @Bean public Binding stockDlqBinding() {
        return BindingBuilder.bind(stockDlq()).to(pedidosDlxExchange()).with(stockDlqKey);
    }
}