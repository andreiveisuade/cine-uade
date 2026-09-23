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
 * Una película del catálogo. Lo que participa de las reglas —título, duración, géneros,
 * clasificación— va en el constructor; los datos de catálogo (director, sinopsis, año...)
 * solo se muestran y se cargan con setters. Un constructor de nueve parámetros se
 * invocaría con los argumentos cambiados sin que nadie lo note.
 *
 * <p>Los géneros son {@code @ElementCollection}: un género es una constante del enum, sin
 * identidad propia.
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

    /** EAGER: ninguna pantalla muestra una película sin sus géneros. */
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

    // Sin decirle DECIMAL(3,1), Hibernate espera FLOAT y `validate` corta el arranque.
    @Column(columnDefinition = "DECIMAL(3,1)")
    private double puntaje;

    private int votos;

    /** Confirmada por defecto: cargarla a mano ya es haberla decidido. Solo el importador la baja a PENDIENTE. */
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
     * Cambia lo que identifica a la película. La edición muta la entidad cargada y no arma
     * otra con el mismo id: con un contexto de persistencia serían dos objetos peleando por
     * la misma fila.
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

    /** De 0 a 10: el {@code vote_average} de TMDB, o cero si se cargó a mano sin dato. Es lo que ordena el planificador. */
    public double getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(double puntaje) {
        this.puntaje = puntaje;
    }

    /** Sobre cuántos votos se calculó el puntaje: un 8,0 sobre seis no es lo mismo que sobre cinco mil. */
    public int getVotos() {
        return votos;
    }

    public void setVotos(int votos) {
        this.votos = votos;
    }

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
