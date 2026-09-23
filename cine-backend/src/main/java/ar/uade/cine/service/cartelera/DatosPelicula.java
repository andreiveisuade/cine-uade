package ar.uade.cine.service.cartelera;

import java.util.List;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;

// Campos objeto: en una edición null es "no lo mandé", distinto de cero o false.
public record DatosPelicula(String titulo, Integer duracionMinutos, List<Genero> generos,
                            Clasificacion clasificacion, String director, String sinopsis,
                            Integer anio, String idiomaOriginal, String posterUrl,
                            Boolean enCartelera, Double puntaje, Integer votos) {

    public static DatosPelicula deAlta(String titulo, int duracionMinutos, List<Genero> generos,
                                       Clasificacion clasificacion) {
        return new DatosPelicula(titulo, duracionMinutos, generos, clasificacion,
                null, null, null, null, null, null, null, null);
    }
}
