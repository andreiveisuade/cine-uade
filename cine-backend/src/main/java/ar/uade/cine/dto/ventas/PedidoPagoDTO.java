package ar.uade.cine.dto.ventas;

import jakarta.validation.constraints.NotBlank;

// Lo que entra al cobrar una reserva (POST /api/reservas/{id}/pago); exige el medio, no el código (R11).
// Sin monto a propósito: sale del total congelado en la reserva.
public record PedidoPagoDTO(@NotBlank(message = "Falta el medio de pago") String medio,
                            String codigoAutorizacion) {
}
