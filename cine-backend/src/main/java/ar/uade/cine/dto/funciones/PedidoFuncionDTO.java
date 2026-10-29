package ar.uade.cine.dto.funciones;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que entra al programar o editar una función (POST y PUT); Bean Validation exige los seis campos.
public record PedidoFuncionDTO(
        @Schema(example = "1")
        @NotNull(message = "Falta la película") Integer peliculaId,
        @Schema(example = "1")
        @NotNull(message = "Falta la sala") Integer salaId,
        @Schema(description = "AAAA-MM-DDTHH:MM, posterior a ahora (R20)", example = "2026-11-06T20:30:00")
        @NotBlank(message = "Falta la fecha y hora de la función") String inicio,
        @Schema(allowableValues = {"DOBLADA", "SUBTITULADA"}, example = "SUBTITULADA")
        @NotBlank(message = "Falta el idioma") String idioma,
        @Schema(allowableValues = {"DOS_D", "TRES_D"}, example = "DOS_D")
        @NotBlank(message = "Falta la proyección") String proyeccion,
        @Schema(example = "6000.00")
        @NotNull(message = "Falta el precio") Double precio) {
}
