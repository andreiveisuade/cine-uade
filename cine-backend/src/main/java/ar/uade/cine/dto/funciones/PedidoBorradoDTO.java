package ar.uade.cine.dto.funciones;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Lo que entra al borrar varias funciones de una vez (POST /api/funciones/borrado); exige la lista.
public record PedidoBorradoDTO(
        @Schema(description = "Ids de las funciones, entre 1 y 500", example = "[12, 13, 14]")
        @NotNull(message = "Faltan las funciones a borrar") List<Integer> ids) {
}
