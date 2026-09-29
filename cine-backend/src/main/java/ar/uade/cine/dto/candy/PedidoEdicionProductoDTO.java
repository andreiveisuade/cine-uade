package ar.uade.cine.dto.candy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que entra al editar un producto o combo (PUT /api/candy/productos/{id}); exige nombre y precio.
// Solo presencia: el precio lo valida Dinero.importe al convertirlo en el controller.
public record PedidoEdicionProductoDTO(@NotBlank(message = "Falta el nombre") String nombre,
                                       @NotNull(message = "Falta el precio") Double precio) {
}
