package ar.uade.cine.dto.ventas;

import jakarta.validation.constraints.NotBlank;

public record PedidoAccesoDTO(@NotBlank(message = "Falta el código de acceso") String codigo) {
}
