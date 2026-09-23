package ar.uade.cine.dto.funciones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoFuncionDTO(@NotNull(message = "Falta la película") Integer peliculaId,
                               @NotNull(message = "Falta la sala") Integer salaId,
                               @NotBlank(message = "Falta la fecha y hora de la función") String inicio,
                               @NotBlank(message = "Falta la versión o el formato de proyección") String idioma,
                               @NotBlank(message = "Falta la versión o el formato de proyección") String proyeccion,
                               @NotNull(message = "El precio debe ser mayor a cero") @Positive(message = "El precio debe ser mayor a cero") Double precio) {
}
