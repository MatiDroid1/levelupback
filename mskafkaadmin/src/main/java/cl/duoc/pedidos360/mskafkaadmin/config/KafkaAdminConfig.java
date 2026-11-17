package cl.duoc.pedidos360.mskafkaadmin.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public AdminClient adminClient(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        Map<String, Object> propiedades = new HashMap<>();

        propiedades.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        propiedades.put(
                AdminClientConfig.CLIENT_ID_CONFIG,
                "mskafkaadmin"
        );

        propiedades.put(
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
                10000
        );

        propiedades.put(
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG,
                15000
        );

        return AdminClient.create(propiedades);
    }
}