package ar.uade.cine.dto.ventas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al abrir un checkout electrónico (POST /api/reservas/{id}/checkout); exige el medio de pago.
public record PedidoCheckoutDTO(
        @Schema(description = "Un medio electrónico: no EFECTIVO", allowableValues = {"EFECTIVO", "DEBITO", "CREDITO", "QR", "TRANSFERENCIA"}, example = "QR")
        @NotBlank(message = "Falta el medio de pago") String medio) {
}
