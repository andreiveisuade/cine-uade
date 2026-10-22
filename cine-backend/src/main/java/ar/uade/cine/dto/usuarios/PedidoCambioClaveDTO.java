package ar.uade.cine.dto.usuarios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al cambiar la clave con sesión (POST /api/cuenta/clave, R24): la actual y la nueva.
public record PedidoCambioClaveDTO(
        @Schema(example = "la-clave-de-hoy")
        @NotBlank(message = "Falta la contraseña actual") String actual,
        @Schema(example = "una-clave-nueva")
        @NotBlank(message = "Falta la contraseña") String nueva) {
}
