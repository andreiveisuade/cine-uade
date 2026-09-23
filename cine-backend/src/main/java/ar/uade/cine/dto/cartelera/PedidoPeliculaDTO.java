package ar.uade.cine.dto.cartelera;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Mismos textos que GestorCartelera: el alta que no pasa por HTTP los sigue viendo desde ahí.
public record PedidoPeliculaDTO(
        @NotBlank(message = "El título no puede estar vacío") String titulo,
        @NotNull(message = "La duración debe ser mayor a cero")
        @Positive(message = "La duración debe ser mayor a cero") Integer duracionMinutos,
        @NotEmpty(message = "La película necesita al menos un género") List<String> generos,
        @NotBlank(message = "Falta la clasificación por edad") String clasificacion,
        String director, String sinopsis, Integer anio,
        String idiomaOriginal, String posterUrl, Boolean enCartelera,
        Double puntaje, Integer votos) {
}
