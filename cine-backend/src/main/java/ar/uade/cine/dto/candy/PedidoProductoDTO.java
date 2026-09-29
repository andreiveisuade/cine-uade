package ar.uade.cine.dto.candy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que entra al dar de alta un producto suelto (POST /api/candy/productos); exige nombre, tipo y precio.
// Solo presencia: el precio lo valida Dinero.importe al convertirlo en el controller.
public record PedidoProductoDTO(@NotBlank(message = "Falta el nombre") String nombre,
                                @NotBlank(message = "Falta el tipo de producto") String tipo,
                                @NotNull(message = "Falta el precio") Double precio) {
}
