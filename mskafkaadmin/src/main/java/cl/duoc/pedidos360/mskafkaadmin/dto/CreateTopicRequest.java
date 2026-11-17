package cl.duoc.pedidos360.mskafkaadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 249, message = "El nombre admite hasta 249 caracteres")
        @Pattern(
                regexp = "[a-zA-Z0-9._-]+",
                message = "El nombre solo admite letras, números, punto, guion y guion bajo"
        )
        String name,

        @NotNull(message = "La cantidad de particiones es obligatoria")
        @Min(value = 1, message = "Debe existir al menos una partición")
        @Max(value = 100, message = "Esta API admite hasta 100 particiones")
        Integer partitions,

        @NotNull(message = "El factor de replicación es obligatorio")
        @Min(value = 1, message = "El factor de replicación mínimo es 1")
        @Max(value = 3, message = "Esta API está preparada para hasta 3 réplicas")
        Short replicationFactor,

        Map<String, String> configs

) {
}