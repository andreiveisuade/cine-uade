package ar.uade.cine.dto.candy;

import jakarta.validation.constraints.NotNull;

public record PedidoDisponibilidadDTO(
        @NotNull(message = "Falta decir si el producto queda disponible") Boolean disponible) {
}
