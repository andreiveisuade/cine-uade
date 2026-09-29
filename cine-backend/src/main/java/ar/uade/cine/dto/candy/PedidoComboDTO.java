package ar.uade.cine.dto.candy;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que entra al armar un combo (POST /api/candy/combos); exige nombre y precio, los componentes el modelo.
// Solo presencia: el precio lo valida Dinero.importe al convertirlo en el controller.
public record PedidoComboDTO(@NotBlank(message = "Falta el nombre") String nombre,
                             @NotNull(message = "Falta el precio") Double precio,
                             Map<Integer, Integer> componentes) {
}
