package ar.uade.cine.dto.candy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Lo que entra al sacar o reponer un producto (PATCH /api/candy/productos/{id}); exige el flag.
public record PedidoDisponibilidadDTO(
        @Schema(description = "false lo saca de la carta sin borrarlo", example = "false")
        @NotNull(message = "Falta decir si el producto queda disponible") Boolean disponible) {
}
