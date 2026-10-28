package ar.uade.cine.dto.candy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al entregar el candy con el QR de la entrada (POST /api/candy/retiros); exige el código.
public record PedidoRetiroDTO(
        @Schema(description = "El código de acceso del ticket", example = "K7M2P9XQ")
        @NotBlank(message = "Falta el código de acceso") String codigo) {
}
