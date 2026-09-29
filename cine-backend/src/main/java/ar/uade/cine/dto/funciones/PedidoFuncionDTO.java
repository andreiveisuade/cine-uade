package ar.uade.cine.dto.funciones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Lo que entra al programar una función (POST /api/funciones); Bean Validation exige los seis campos.
public record PedidoFuncionDTO(@NotNull(message = "Falta la película") Integer peliculaId,
                               @NotNull(message = "Falta la sala") Integer salaId,
                               @NotBlank(message = "Falta la fecha y hora de la función") String inicio,
                               @NotBlank(message = "Falta el idioma") String idioma,
                               @NotBlank(message = "Falta la proyección") String proyeccion,
                               @NotNull(message = "Falta el precio") @Positive(message = "El precio tiene que ser mayor a cero") Double precio) {
}
