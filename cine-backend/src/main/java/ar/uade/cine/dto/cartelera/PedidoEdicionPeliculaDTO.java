package ar.uade.cine.dto.cartelera;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Lo que entra al editar una película (PUT /api/peliculas/{id}); parcial: lo que no viaja queda igual.
public record PedidoEdicionPeliculaDTO(
        @Schema(example = "Matrix")
        String titulo, 
        @Schema(description = "De 1 a 600", example = "136")
        Integer duracionMinutos, 
        @Schema(description = "Valores de GET /api/generos", example = "[\"ACCION\"]")
        List<String> generos,
        @Schema(allowableValues = {"ATP", "MAS_13", "MAS_16", "MAS_18"}, example = "MAS_16")
        String clasificacion, 
        @Schema(example = "Lana y Lilly Wachowski")
        String director, 
        @Schema(example = "Un programador descubre una simulación.")
        String sinopsis,
        @Schema(example = "1999")
        Integer anio, 
        @Schema(example = "Inglés")
        String idiomaOriginal, 
        @Schema(description = "Empieza con http:// o https://", example = "https://image.tmdb.org/t/p/w500/matrix.jpg")
        String posterUrl,
        @Schema(example = "true")
        Boolean enCartelera, 
        @Schema(description = "De 0 a 10", example = "8.2")
        Double puntaje, 
        @Schema(example = "26000")
        Integer votos) {
}
