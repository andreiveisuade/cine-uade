package ar.uade.cine.swing.api.dto.cartelera;

import java.util.List;

// Una película como la lee Swing, sola en el catálogo o embebida en funciones, reservas y pagos.
public record Pelicula(int id, String titulo, int duracionMinutos, List<String> generos, String clasificacion,
                       String posterUrl, String director, int anio, String idiomaOriginal, String sinopsis,
                       boolean enCartelera) {
}
