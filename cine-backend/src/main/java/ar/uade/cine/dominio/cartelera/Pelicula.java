package ar.uade.cine.dominio.cartelera;

import java.util.List;

/**
 * Una película del catálogo. Lo que la identifica en el negocio —título, duración,
 * géneros, clasificación— va separado de los datos de catálogo de más abajo (director,
 * sinopsis, año...), que se muestran pero no participan de ninguna regla.
 */
public interface Pelicula {

    int getId();

    /** Lo usa el DAO para asignar el id que genera la base. */
    void setId(int id);

    String getTitulo();

    int getDuracionMinutos();

    List<Genero> getGeneros();

    void agregarGenero(Genero genero);

    Clasificacion getClasificacion();

    // --- datos de catálogo: para mostrar la película, sin reglas asociadas ---

    String getDirector();

    void setDirector(String director);

    String getSinopsis();

    void setSinopsis(String sinopsis);

    int getAnio();

    void setAnio(int anio);

    /** Idioma hablado en la película, distinto de si la función va doblada o subtitulada. */
    String getIdiomaOriginal();

    void setIdiomaOriginal(String idiomaOriginal);

    String getPosterUrl();

    void setPosterUrl(String posterUrl);

    /** Una película cargada no necesariamente sigue en cartelera. */
    boolean estaEnCartelera();

    void setEnCartelera(boolean enCartelera);
}
