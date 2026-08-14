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

    /**
     * Qué tan bien valorada está, de 0 a 10. Es el {@code vote_average} de TMDB para lo
     * importado, y cero para lo que se carga a mano sin dato.
     *
     * <p>Existe para que el planificador de la grilla pueda ordenar: sin un número que
     * compare dos películas, "programar las mejores" no se puede resolver.
     */
    double getPuntaje();

    void setPuntaje(double puntaje);

    /**
     * Sobre cuántos votos se calculó el puntaje.
     *
     * <p>Es lo que dice cuánto vale ese puntaje, y sin esto no se puede leer. Un 8,0 sobre
     * seis votos y un 8,0 sobre cinco mil son el mismo número y no la misma información; un
     * 0,0 sobre cero votos no es una película mala, es una que todavía nadie vio.
     */
    int getVotos();

    void setVotos(int votos);

    /**
     * Si el encargado ya decidió qué hacer con ella. Las que carga él nacen
     * {@link EstadoRevision#CONFIRMADA} —cargarla ya es haberla decidido— y las que trae
     * el importador nacen {@link EstadoRevision#PENDIENTE}.
     */
    EstadoRevision getEstadoRevision();

    void setEstadoRevision(EstadoRevision estadoRevision);
}
