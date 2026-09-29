package ar.uade.cine.dto.candy;

import jakarta.validation.constraints.NotNull;

// Lo que entra al sacar o reponer un producto (PATCH /api/candy/productos/{id}); exige el flag.
public record PedidoDisponibilidadDTO(
        @NotNull(message = "Falta decir si el producto queda disponible") Boolean disponible) {
}
