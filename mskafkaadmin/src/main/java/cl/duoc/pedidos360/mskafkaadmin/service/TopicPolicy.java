package cl.duoc.pedidos360.mskafkaadmin.service;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TopicPolicy {

    private static final Set<String> PROTECTED_TOPICS = Set.of(
            "pedidos.eventos",
            "stock.movimientos"
    );

    private static final Set<String> ALLOWED_CONFIGS = Set.of(
            "retention.ms",
            "retention.bytes",
            "cleanup.policy",
            "min.insync.replicas",
            "segment.ms"
    );

    private final String writablePrefix;

    public TopicPolicy(
            @Value("${app.kafka.writable-prefix:demo.}")
            String writablePrefix) {

        if (writablePrefix == null || writablePrefix.isBlank()) {
            throw new IllegalArgumentException(
                    "El prefijo de escritura no puede estar vacío"
            );
        }

        this.writablePrefix = writablePrefix;
    }

    public void validateName(String name) {

        if (name == null
                || name.length() > 249
                || !name.matches("[a-zA-Z0-9._-]+")
                || name.equals(".")
                || name.equals("..")) {

            throw new IllegalArgumentException(
                    "El nombre del tópico es inválido"
            );
        }
    }

    public void validateWritable(String name) {

        validateName(name);

        if (name.startsWith("__")
                || PROTECTED_TOPICS.contains(name)
                || !name.startsWith(writablePrefix)) {

            throw new IllegalArgumentException(
                    "Solo se permiten escrituras sobre tópicos "
                            + writablePrefix
                            + "*; los tópicos de negocio están protegidos"
            );
        }
    }

    public void validateConfigs(Map<String, String> configs) {

        if (configs == null) {
            return;
        }

        configs.forEach((key, value) -> {

            if (key == null || !ALLOWED_CONFIGS.contains(key)) {
                throw new IllegalArgumentException(
                        "Configuración no permitida: " + key
                );
            }

            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "El valor de " + key + " no puede estar vacío"
                );
            }

            if (key.equals("cleanup.policy")) {

                if (!Set.of(
                        "delete",
                        "compact",
                        "compact,delete",
                        "delete,compact"
                ).contains(value)) {

                    throw new IllegalArgumentException(
                            "cleanup.policy debe ser delete, compact o compact,delete"
                    );
                }

                return;
            }

            long number;

            try {
                number = Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "El valor de " + key + " debe ser numérico"
                );
            }

            switch (key) {

                case "min.insync.replicas" -> {
                    if (number < 1 || number > 3) {
                        throw new IllegalArgumentException(
                                "min.insync.replicas debe estar entre 1 y 3"
                        );
                    }
                }

                case "segment.ms" -> {
                    if (number < 1) {
                        throw new IllegalArgumentException(
                                "segment.ms debe ser mayor que cero"
                        );
                    }
                }

                default -> {
                    if (number < -1) {
                        throw new IllegalArgumentException(
                                key + " debe ser mayor o igual que -1"
                        );
                    }
                }
            }
        });
    }
}