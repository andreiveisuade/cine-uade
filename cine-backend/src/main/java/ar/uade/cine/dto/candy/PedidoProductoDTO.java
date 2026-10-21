package ar.uade.cine.dto.candy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que entra al dar de alta un producto suelto (POST /api/candy/productos); exige nombre, tipo y precio.
public record PedidoProductoDTO(
        @Schema(description = "Único en la carta", example = "Sugus")
        @NotBlank(message = "Falta el nombre") String nombre,
        @Schema(description = "Un COMBO no va acá: se arma en POST /api/candy/combos", allowableValues = {"POCHOCLOS", "BEBIDA", "GOLOSINA"}, example = "GOLOSINA")
        @NotBlank(message = "Falta el tipo de producto") String tipo,
        @Schema(description = "Mayor a 0", example = "8500.00")
        @NotNull(message = "Falta el precio") Double precio,
        @Schema(description = "La url que devolvió POST /api/imagenes; vacía o null es sin imagen")
        String imagenUrl) {
}
