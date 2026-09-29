package ar.uade.cine.dto.candy;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Lo que entra al armar un combo (POST /api/candy/combos); exige nombre y precio, los componentes el gestor.
public record PedidoComboDTO(@NotBlank(message = "El nombre no puede estar vacío") String nombre,
                             @NotNull(message = "Falta el precio") @Positive(message = "El precio tiene que ser mayor a cero") Double precio,
                             Map<Integer, Integer> componentes) {
}
