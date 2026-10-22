package ar.uade.cine.dto.usuarios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al modificar la cuenta (PUT /api/cuenta, R23); solo el nombre: el email no cambia.
public record PedidoCuentaDTO(
        @Schema(example = "Ana Pérez")
        @NotBlank(message = "Falta el nombre") String nombre) {
}
