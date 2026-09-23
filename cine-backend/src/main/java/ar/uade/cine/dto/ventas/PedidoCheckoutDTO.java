package ar.uade.cine.dto.ventas;

import jakarta.validation.constraints.NotBlank;

public record PedidoCheckoutDTO(@NotBlank(message = "Falta el medio de pago") String medio) {
}
