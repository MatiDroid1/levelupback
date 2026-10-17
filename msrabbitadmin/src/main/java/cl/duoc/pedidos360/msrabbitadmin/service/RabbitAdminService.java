package cl.duoc.pedidos360.msrabbitadmin.service;

import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cl.duoc.pedidos360.msrabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.msrabbitadmin.dto.ExchangeRequest;
import cl.duoc.pedidos360.msrabbitadmin.dto.QueueRequest;
import cl.duoc.pedidos360.msrabbitadmin.exception.RecursoNoEncontradoException;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    // ---------- Colas ----------
    public Map<String, Object> crearCola(QueueRequest req) {
        if (StringUtils.hasText(req.deadLetterRoutingKey()) && !StringUtils.hasText(req.deadLetterExchange())) {
            throw new IllegalArgumentException("deadLetterRoutingKey requiere deadLetterExchange");
        }
        boolean durable = req.durable() == null || req.durable();
        QueueBuilder builder = durable ? QueueBuilder.durable(req.name()) : QueueBuilder.nonDurable(req.name());
        if (StringUtils.hasText(req.deadLetterExchange())) {
            builder.deadLetterExchange(req.deadLetterExchange());
            if (StringUtils.hasText(req.deadLetterRoutingKey())) {
                builder.deadLetterRoutingKey(req.deadLetterRoutingKey());
            }
        }
        Queue cola = builder.build();
        amqpAdmin.declareQueue(cola);
        return Map.of("name", req.name(), "durable", durable, "mensaje", "Cola creada");
    }

    public Map<String, Object> consultarCola(String name) {
        QueueInformation info = amqpAdmin.getQueueInfo(name);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + name + "' no existe");
        }
        return Map.of("name", info.getName(),
                "mensajes", info.getMessageCount(),
                "consumidores", info.getConsumerCount());
    }

    public void eliminarCola(String name) {
        if (amqpAdmin.getQueueInfo(name) == null) {
            throw new RecursoNoEncontradoException("La cola '" + name + "' no existe");
        }
        amqpAdmin.deleteQueue(name);
    }

    // ---------- Exchanges ----------
    public Map<String, Object> crearExchange(ExchangeRequest req) {
        boolean durable = req.durable() == null || req.durable();
        Exchange exchange = switch (req.type()) {
            case "direct" -> new DirectExchange(req.name(), durable, false);
            case "topic" -> new TopicExchange(req.name(), durable, false);
            case "fanout" -> new FanoutExchange(req.name(), durable, false);
            case "headers" -> new HeadersExchange(req.name(), durable, false);
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + req.type());
        };
        amqpAdmin.declareExchange(exchange);
        return Map.of("name", req.name(), "type", req.type(), "durable", durable, "mensaje", "Exchange creado");
    }

    public void eliminarExchange(String name) {
        if (name.startsWith("amq.")) {
            throw new IllegalArgumentException("No se pueden eliminar exchanges reservados (amq.*)");
        }
        amqpAdmin.deleteExchange(name);
    }

    // ---------- Bindings ----------
    public Map<String, Object> crearBinding(BindingRequest req) {
        amqpAdmin.declareBinding(construirBinding(req));
        return Map.of("queue", req.queue(), "exchange", req.exchange(),
                "routingKey", clave(req), "mensaje", "Binding creado");
    }

    public void eliminarBinding(BindingRequest req) {
        amqpAdmin.removeBinding(construirBinding(req));
    }

    private Binding construirBinding(BindingRequest req) {
        return new Binding(req.queue(), Binding.DestinationType.QUEUE, req.exchange(), clave(req), null);
    }

    private String clave(BindingRequest req) {
        return req.routingKey() == null ? "" : req.routingKey();
    }
}