package ar.uade.cine.dto.candy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que entra al editar un producto o combo (PUT /api/candy/productos/{id}); exige nombre y precio.
public record PedidoEdicionProductoDTO(
        @Schema(example = "Pochoclos medianos")
        @NotBlank(message = "Falta el nombre") String nombre,
        @Schema(description = "Mayor a 0", example = "5000.00")
        @NotNull(message = "Falta el precio") Double precio,
        @Schema(description = "La url que devolvió POST /api/imagenes; vacía o null es sin imagen")
        String imagenUrl) {
}
