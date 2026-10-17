package cl.duoc.pedidos360.msrabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QueueRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = Reglas.NOMBRE, message = Reglas.MENSAJE_NOMBRE)
        String name,
        Boolean durable,
        String deadLetterExchange,
        String deadLetterRoutingKey) {
}