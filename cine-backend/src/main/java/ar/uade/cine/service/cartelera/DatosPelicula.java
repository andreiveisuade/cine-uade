package ar.uade.cine.service.cartelera;

import java.util.List;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;

/**
 * Datos de alta o edición de una película. Campos objeto porque en una edición {@code null}
 * es "no lo mandé", distinto de cero o false. Vive en service porque el importador y la
 * API cargan películas igual.
 */
public record DatosPelicula(String titulo, Integer duracionMinutos, List<Genero> generos,
                            Clasificacion clasificacion, String director, String sinopsis,
                            Integer anio, String idiomaOriginal, String posterUrl,
                            Boolean enCartelera, Double puntaje, Integer votos) {

    /** Los datos de catálogo se pueden completar después. */
    public static DatosPelicula deAlta(String titulo, int duracionMinutos, List<Genero> generos,
                                       Clasificacion clasificacion) {
        return new DatosPelicula(titulo, duracionMinutos, generos, clasificacion,
                null, null, null, null, null, null, null, null);
    }
}
