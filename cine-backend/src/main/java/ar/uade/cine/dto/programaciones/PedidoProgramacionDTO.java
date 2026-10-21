package ar.uade.cine.dto.programaciones;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que entra al crear o previsualizar una programación (POST /api/programaciones); sin hasta queda abierta.
public record PedidoProgramacionDTO(
        @Schema(example = "1")
        @NotNull(message = "Falta la película") Integer peliculaId,
        @Schema(example = "1")
        @NotNull(message = "Falta la sala") Integer salaId,
        @Schema(description = "AAAA-MM-DD", example = "2026-11-01")
        @NotBlank(message = "Falta la fecha de inicio") String desde,
        @Schema(description = "AAAA-MM-DD; sin el campo queda abierta", example = "2026-11-30")
        String hasta,
        @Schema(description = "HH:MM", example = "20:30")
        @NotBlank(message = "Falta la hora de la función") String horaInicio,
        @Schema(description = "Nombres en inglés de java.time.DayOfWeek; vacío son todos", allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}, example = "[\"FRIDAY\", \"SATURDAY\"]")
        List<String> diasSemana,
        @Schema(allowableValues = {"DOBLADA", "SUBTITULADA"}, example = "SUBTITULADA")
        @NotBlank(message = "Falta el idioma") String idioma,
        @Schema(allowableValues = {"DOS_D", "TRES_D"}, example = "DOS_D")
        @NotBlank(message = "Falta la proyección") String proyeccion,
        @Schema(description = "Base de la entrada; el backend le aplica los multiplicadores", example = "6000.00")
        @NotNull(message = "Falta el precio") Double precio) {
}
