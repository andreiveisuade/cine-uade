package ar.uade.cine.dto.candy;

import jakarta.validation.constraints.NotNull;

// Lo que entra al sacar o reponer un producto (PUT /api/candy/productos/{id}/disponibilidad); exige el flag.
public record PedidoDisponibilidadDTO(
        @NotNull(message = "Falta decir si el producto queda disponible") Boolean disponible) {
}
