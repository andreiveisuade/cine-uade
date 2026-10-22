package ar.uade.cine.dto.usuarios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al recuperar la clave sin sesión (POST /api/cuenta/recuperacion, R25).
public record PedidoRecuperacionDTO(
        @Schema(example = "ana@example.com")
        @NotBlank(message = "Falta el email") String email,
        @Schema(example = "K7M2QX9P")
        @NotBlank(message = "Falta el código de una de tus entradas") String codigoReserva,
        @Schema(example = "una-clave-nueva")
        @NotBlank(message = "Falta la contraseña") String nueva) {
}
