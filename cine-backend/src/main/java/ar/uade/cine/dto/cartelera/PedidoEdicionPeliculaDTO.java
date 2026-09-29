package ar.uade.cine.dto.cartelera;

import java.util.List;

// Lo que entra al editar una película (PUT /api/peliculas/{id}); parcial: lo que no viaja queda igual.
public record PedidoEdicionPeliculaDTO(String titulo, Integer duracionMinutos, List<String> generos,
                                       String clasificacion, String director, String sinopsis,
                                       Integer anio, String idiomaOriginal, String posterUrl,
                                       Boolean enCartelera, Double puntaje, Integer votos) {
}
