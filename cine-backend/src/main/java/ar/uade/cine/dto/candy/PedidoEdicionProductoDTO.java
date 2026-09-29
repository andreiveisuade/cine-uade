package ar.uade.cine.dto.candy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Lo que entra al editar un producto o combo (PUT /api/candy/productos/{id}); exige nombre y precio > 0.
public record PedidoEdicionProductoDTO(@NotBlank(message = "El nombre no puede estar vacío") String nombre,
                                       @NotNull(message = "El precio debe ser mayor a cero") @Positive(message = "El precio debe ser mayor a cero") Double precio) {
}
