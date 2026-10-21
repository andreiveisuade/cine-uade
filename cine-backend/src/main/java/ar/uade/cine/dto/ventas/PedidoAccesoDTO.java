package ar.uade.cine.dto.ventas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al validar el QR en la puerta (POST /api/acceso); Bean Validation exige el código.
public record PedidoAccesoDTO(
        @Schema(description = "El código de acceso del ticket", example = "ABC123")
        @NotBlank(message = "Falta el código de acceso") String codigo) {
}
