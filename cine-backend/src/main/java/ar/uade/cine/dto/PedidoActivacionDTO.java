package ar.uade.cine.dto;

import jakarta.validation.constraints.NotNull;

public record PedidoActivacionDTO(
        @NotNull(message = "Falta decir si queda activa") Boolean activa) {
}
