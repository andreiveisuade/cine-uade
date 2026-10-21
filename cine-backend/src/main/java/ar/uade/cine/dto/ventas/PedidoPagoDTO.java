package ar.uade.cine.dto.ventas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al cobrar una reserva (POST /api/reservas/{id}/pago); exige el medio, no el código (R11).
public record PedidoPagoDTO(
        @Schema(allowableValues = {"EFECTIVO", "DEBITO", "CREDITO", "QR", "TRANSFERENCIA"}, example = "EFECTIVO")
        @NotBlank(message = "Falta el medio de pago") String medio,
        @Schema(description = "Solo si el medio lo requiere (GET /api/medios-pago)")
        String codigoAutorizacion) {
}
