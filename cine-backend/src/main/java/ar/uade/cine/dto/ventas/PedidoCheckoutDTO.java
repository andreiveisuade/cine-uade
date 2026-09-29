package ar.uade.cine.dto.ventas;

import jakarta.validation.constraints.NotBlank;

// Lo que entra al abrir un checkout electrónico (POST /api/reservas/{id}/checkout); exige el medio de pago.
public record PedidoCheckoutDTO(@NotBlank(message = "Falta el medio de pago") String medio) {
}
