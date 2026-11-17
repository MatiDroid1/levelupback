package cl.duoc.pedidos360.mskafkaadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UpdateConfigsRequest(

        @NotEmpty(message = "Debes indicar al menos una configuración")
        Map<
                @NotBlank(message = "La clave no puede estar vacía") String,
                @NotBlank(message = "El valor no puede estar vacío") String
        > configs

) {
}