package ar.uade.cine.dto.usuarios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al registrar un cliente (POST /api/clientes); exige nombre, email y clave, que valida el modelo.
// codigoReserva solo hace falta si el email ya compró sin cuenta (R22).
public record PedidoClienteDTO(
        @Schema(example = "Ana Pérez")
        @NotBlank(message = "Falta el nombre") String nombre,
        @Schema(example = "ana@example.com")
        @NotBlank(message = "Falta el email") String email,
        @Schema(example = "una-clave-larga")
        @NotBlank(message = "Falta la contraseña") String password,
        @Schema(example = "K7M2QX9P", requiredMode = Schema.RequiredMode.NOT_REQUIRED) String codigoReserva) {
}
