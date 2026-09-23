package ar.uade.cine.dto.candy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoProductoDTO(@NotBlank(message = "El nombre no puede estar vacío") String nombre,
                                @NotBlank(message = "Falta el tipo de producto") String tipo,
                                @NotNull(message = "El precio debe ser mayor a cero") @Positive(message = "El precio debe ser mayor a cero") Double precio) {
}
