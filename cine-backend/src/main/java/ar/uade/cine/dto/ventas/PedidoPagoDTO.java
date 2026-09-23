package ar.uade.cine.dto.ventas;

import jakarta.validation.constraints.NotBlank;

// Sin monto a propósito: sale del total congelado en la reserva.
public record PedidoPagoDTO(@NotBlank(message = "Falta el medio de pago") String medio,
                            String codigoAutorizacion) {
}
