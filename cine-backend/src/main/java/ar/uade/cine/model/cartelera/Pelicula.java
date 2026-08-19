package ar.uade.cine.model.cartelera;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * Una película del catálogo. Lo que la identifica en el negocio —título, duración,
 * géneros, clasificación— va separado de los datos de catálogo de más abajo (director,
 * sinopsis, año...), que se muestran pero no participan de ninguna regla.
 *
 * <p>Lo que identifica a la película va en el constructor; los datos de catálogo se cargan
 * después con setters. Un constructor de nueve parámetros sería imposible de leer y
 * facilísimo de invocar con los argumentos cambiados de orden.
 *
 * <p>Los géneros son una {@code @ElementCollection} y no una entidad: un género no tiene
 * identidad propia ni vida fuera de la película —es una constante del enum— y por eso la
 * tabla {@code pelicula_genero} guarda el nombre de la constante y nada más. Es la misma
 * decisión que ya estaba tomada en el schema, ahora dicha en el mapeo.
 */
@Entity
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String titulo;

    private int duracionMinutos;

    @Enumerated(EnumType.STRING)
    private Clasificacion clasificacion;

    /**
     * EAGER y no LAZY: no hay ninguna pantalla que muestre una película sin sus géneros
     * —la cartelera los pinta como etiquetas y el planificador de la grilla los usa para
     * repartir— así que diferirlos solo agregaría una consulta por película y el riesgo de
     * tocarlos fuera de la sesión.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pelicula_genero",
            joinColumns = @JoinColumn(name = "pelicula_id"))
    @Column(name = "genero")
    @Enumerated(EnumType.STRING)
    private List<Genero> generos = new ArrayList<>();

    private String director = "";

    private String sinopsis = "";

    private int anio;

    private String idiomaOriginal = "";

    private String posterUrl = "";

    private boolean enCartelera = true;

    private double puntaje;

    private int votos;

    /**
     * Confirmada por defecto: el alta normal es la del encargado, y cargarla a mano ya es
     * haberla decidido. Sólo el importador la baja a PENDIENTE.
     */
    @Enumerated(EnumType.STRING)
    private EstadoRevision estadoRevision = EstadoRevision.CONFIRMADA;

    protected Pelicula() {
    }

    /** Película nueva: todavía no tiene id, lo asigna la base al guardarla. */
    public Pelicula(String titulo, int duracionMinutos, List<Genero> generos,
                    Clasificacion clasificacion) {
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.clasificacion = clasificacion;
        this.generos.addAll(generos);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public Clasificacion getClasificacion() {
        return clasificacion;
    }

    /** Copia defensiva: nadie modifica la lista interna desde afuera. */
    public List<Genero> getGeneros() {
        return new ArrayList<>(generos);
    }

    public void agregarGenero(Genero genero) {
        if (!generos.contains(genero)) {
            generos.add(genero);
        }
    }

    /**
     * Cambia lo que identifica a la película: título, duración, géneros y clasificación.
     *
     * <p>Existe desde que la edición muta la entidad cargada en vez de armar otra con el
     * mismo id. Eso último era lo que hacía falta cuando un DAO escribía el UPDATE a mano;
     * con un contexto de persistencia de por medio son dos objetos peleando por la misma
     * fila, y el que gana es el que Hibernate tiene adentro.
     */
    public void actualizar(String titulo, int duracionMinutos, List<Genero> generos,
                           Clasificacion clasificacion) {
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.clasificacion = clasificacion;
        this.generos.clear();
        generos.forEach(this::agregarGenero);
    }

    // --- datos de catálogo: para mostrar la película, sin reglas asociadas ---

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    /** Idioma hablado en la película, distinto de si la función va doblada o subtitulada. */
    public String getIdiomaOriginal() {
        return idiomaOriginal;
    }

    public void setIdiomaOriginal(String idiomaOriginal) {
        this.idiomaOriginal = idiomaOriginal;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    /** Una película cargada no necesariamente sigue en cartelera. */
    public boolean estaEnCartelera() {
        return enCartelera;
    }

    public void setEnCartelera(boolean enCartelera) {
        this.enCartelera = enCartelera;
    }

    /**
     * Qué tan bien valorada está, de 0 a 10. Es el {@code vote_average} de TMDB para lo
     * importado, y cero para lo que se carga a mano sin dato.
     *
     * <p>Existe para que el planificador de la grilla pueda ordenar: sin un número que
     * compare dos películas, "programar las mejores" no se puede resolver.
     */
    public double getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(double puntaje) {
        this.puntaje = puntaje;
    }

    /**
     * Sobre cuántos votos se calculó el puntaje.
     *
     * <p>Es lo que dice cuánto vale ese puntaje, y sin esto no se puede leer. Un 8,0 sobre
     * seis votos y un 8,0 sobre cinco mil son el mismo número y no la misma información; un
     * 0,0 sobre cero votos no es una película mala, es una que todavía nadie vio.
     */
    public int getVotos() {
        return votos;
    }

    public void setVotos(int votos) {
        this.votos = votos;
    }

    /**
     * Si el encargado ya decidió qué hacer con ella. Las que carga él nacen
     * {@link EstadoRevision#CONFIRMADA} —cargarla ya es haberla decidido— y las que trae el
     * importador nacen {@link EstadoRevision#PENDIENTE}.
     */
    public EstadoRevision getEstadoRevision() {
        return estadoRevision;
    }

    public void setEstadoRevision(EstadoRevision estadoRevision) {
        this.estadoRevision = estadoRevision;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + titulo + " (" + duracionMinutos + " min) "
                + clasificacion.getEtiqueta() + " " + generos
                + (enCartelera ? "" : " - fuera de cartelera");
    }
}
