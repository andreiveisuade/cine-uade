package ar.uade.cine.dto.programaciones;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoProgramacionDTO(@NotNull(message = "Falta la película") Integer peliculaId,
                                    @NotNull(message = "Falta la sala") Integer salaId,
                                    @NotBlank(message = "Falta la fecha de inicio") String desde,
                                    String hasta,
                                    @NotBlank(message = "Falta la hora de la función") String horaInicio,
                                    List<String> diasSemana,
                                    @NotBlank(message = "Falta el idioma") String idioma,
                                    @NotBlank(message = "Falta la proyección") String proyeccion,
                                    @NotNull(message = "El precio debe ser mayor a cero") @Positive(message = "El precio debe ser mayor a cero") Double precio) {
}
