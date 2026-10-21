package ar.uade.cine.dto.cartelera;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// Lo que entra al crear una película (POST /api/peliculas); exige título, duración, géneros y clasificación.
// Solo presencia, con los textos de ValidadorPelicula; el resto lo aplica la película. Los enums viajan como String.
public record PedidoPeliculaDTO(
        @Schema(description = "Único en el catálogo, hasta 100 caracteres", example = "Matrix")
        @NotBlank(message = "Falta el título") String titulo,
        @Schema(description = "De 1 a 600 minutos", example = "136")
        @NotNull(message = "Falta la duración") Integer duracionMinutos,
        @Schema(description = "Al menos uno; los repetidos se juntan",
                allowableValues = {"ACCION", "COMEDIA", "DRAMA", "TERROR", "CIENCIA_FICCION", "ANIMACION",
                        "DOCUMENTAL", "ROMANCE"},
                example = "[\"ACCION\", \"CIENCIA_FICCION\"]")
        @NotEmpty(message = "La película tiene que tener al menos un género") List<String> generos,
        @Schema(description = "Edad mínima", allowableValues = {"ATP", "MAS_13", "MAS_16", "MAS_18"},
                example = "MAS_16")
        @NotBlank(message = "Falta la clasificación por edad") String clasificacion,
        @Schema(description = "Hasta 100 caracteres", example = "Lana y Lilly Wachowski") String director,
        @Schema(description = "Hasta 5000 caracteres",
                example = "Un programador descubre que su realidad es una simulación.") String sinopsis,
        @Schema(description = "Desde 1895", example = "1999") Integer anio,
        @Schema(description = "Hasta 40 caracteres", example = "Inglés") String idiomaOriginal,
        @Schema(description = "Empieza con http:// o https://; hasta 255 caracteres",
                example = "https://image.tmdb.org/t/p/w500/matrix.jpg") String posterUrl,
        @Schema(description = "Si sale en la cartelera pública apenas se crea", example = "true")
        Boolean enCartelera,
        @Schema(description = "De 0 a 10", example = "8.2") Double puntaje,
        @Schema(description = "De 0 a 100.000.000", example = "26000") Integer votos,
        @Schema(description = "Id del video de YouTube o su URL (watch?v=, youtu.be/, /embed/); se guarda el id",
                example = "https://www.youtube.com/watch?v=vKQi3bBA1y8") String trailerYoutube) {
}
