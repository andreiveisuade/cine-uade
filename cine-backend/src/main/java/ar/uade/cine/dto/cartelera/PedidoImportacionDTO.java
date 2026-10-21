package ar.uade.cine.dto.cartelera;

import io.swagger.v3.oas.annotations.media.Schema;

// El cuerpo opcional de POST /api/importaciones; sin paginas el gestor trae una, fuera de 1 a 3 rechaza.
public record PedidoImportacionDTO(
        @Schema(description = "De 1 a 3; sin el campo trae una", example = "1")
        Integer paginas) {
}
