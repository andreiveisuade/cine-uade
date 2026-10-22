package ar.uade.cine.dto.usuarios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al dar de baja la cuenta (DELETE /api/cuenta, R26): la clave actual confirma que es el dueño.
public record PedidoBajaDTO(
        @Schema(example = "la-clave-de-hoy")
        @NotBlank(message = "Falta la contraseña actual") String actual) {
}
