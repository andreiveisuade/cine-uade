package ar.uade.cine.service.cartelera;

import java.util.List;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;

/**
 * Lo que hace falta para dar de alta o editar una película.
 *
 * <p>Los campos van en tipos de objeto —Integer, Boolean— a propósito: en una edición
 * {@code null} significa "esto no lo mandé, dejalo como está", que es distinto de mandar
 * cero o false.
 *
 * <p>Vive en la capa de servicio y no en la de HTTP porque el importador carga películas
 * igual que la API. Si la forma del pedido fuera un record de <code>api/</code>, cada
 * interfaz tendría que armar la película editada por su cuenta, y las dos copias de esa
 * regla se irían separando.
 */
public record DatosPelicula(String titulo, Integer duracionMinutos, List<Genero> generos,
                            Clasificacion clasificacion, String director, String sinopsis,
                            Integer anio, String idiomaOriginal, String posterUrl,
                            Boolean enCartelera, Double puntaje, Integer votos) {

    /** Lo mínimo de un alta: los datos de catálogo se pueden completar después. */
    public static DatosPelicula deAlta(String titulo, int duracionMinutos, List<Genero> generos,
                                       Clasificacion clasificacion) {
        return new DatosPelicula(titulo, duracionMinutos, generos, clasificacion,
                null, null, null, null, null, null, null, null);
    }

    /** Solo los datos de catálogo, para completarlos sin tocar lo demás. */
    public static DatosPelicula deCatalogo(String director, String sinopsis, Integer anio,
                                           String idiomaOriginal, String posterUrl) {
        return new DatosPelicula(null, null, null, null, director, sinopsis, anio,
                idiomaOriginal, posterUrl, null, null, null);
    }
}
