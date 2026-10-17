package cl.duoc.pedidos360.msrabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = Reglas.NOMBRE, message = Reglas.MENSAJE_NOMBRE)
        String name,
        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "direct|topic|fanout|headers", message = "El tipo debe ser direct, topic, fanout o headers")
        String type,
        Boolean durable) {
}