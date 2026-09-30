package ar.uade.cine.dto.cartelera;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// Lo que entra al crear una película (POST /api/peliculas); exige título, duración, géneros y clasificación.
// Solo presencia, con los mismos textos que ValidadorPelicula: el rango de la duración y el resto de las
// reglas las aplica la película, también en el alta que no pasa por HTTP.
public record PedidoPeliculaDTO(
        @NotBlank(message = "Falta el título") String titulo,
        @NotNull(message = "Falta la duración") Integer duracionMinutos,
        @NotEmpty(message = "La película tiene que tener al menos un género") List<String> generos,
        @NotBlank(message = "Falta la clasificación por edad") String clasificacion,
        String director, String sinopsis, Integer anio,
        String idiomaOriginal, String posterUrl, Boolean enCartelera,
        Double puntaje, Integer votos) {
}
