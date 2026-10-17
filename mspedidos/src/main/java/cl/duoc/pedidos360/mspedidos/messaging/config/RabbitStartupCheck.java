package cl.duoc.pedidos360.mspedidos.messaging.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitStartupCheck {

    @Bean
    public ApplicationRunner verificarConexionRabbit(ConnectionFactory cf) {
        return args -> cf.createConnection().close();
    }
}