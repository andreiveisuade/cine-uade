package ar.uade.cine.dto.programaciones;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Lo que entra a la grilla automática (POST /api/grilla y /propuesta); solo el precio no tiene default.
// Sin proyección: el formato de cada pase lo deciden la sala y la película (R8, R31).
public record PedidoGrillaDTO(
        @Schema(description = "AAAA-MM-DD; sin el campo, mañana", example = "2026-11-01")
        String desde, 
        @Schema(example = "7")
        Integer dias, 
        @Schema(description = "HH:MM", example = "14:00")
        String apertura, 
        @Schema(description = "HH:MM", example = "23:30")
        String cierre,
        @Schema(example = "3")
        Integer cuantasPeliculas,
        @Schema(example = "6000.00")
        @NotNull(message = "Falta el precio") Double precio,
        @Schema(allowableValues = {"DOBLADA", "SUBTITULADA"}, example = "SUBTITULADA")
        String idioma) {
}
