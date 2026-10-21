package ar.uade.cine.dto.candy;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que entra al armar un combo (POST /api/candy/combos); exige nombre y precio, los componentes el modelo.
public record PedidoComboDTO(
        @Schema(description = "Único en la carta", example = "Combo pareja")
        @NotBlank(message = "Falta el nombre") String nombre,
        @Schema(description = "Menor a la suma de los componentes sueltos (R14)", example = "9000.00")
        @NotNull(message = "Falta el precio") Double precio,
        @Schema(description = "id de producto → cantidad: dos productos o más, ninguno combo, 1 a 20 de cada uno", example = "{\"1\": 2, \"2\": 2}")
        Map<Integer, Integer> componentes) {
}
