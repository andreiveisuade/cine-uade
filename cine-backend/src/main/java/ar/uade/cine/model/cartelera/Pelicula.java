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

@Entity
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String titulo;

    private int duracionMinutos;

    @Enumerated(EnumType.STRING)
    private Clasificacion clasificacion;

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

    @Enumerated(EnumType.STRING)
    private EstadoRevision estadoRevision = EstadoRevision.CONFIRMADA;

    protected Pelicula() {
    }

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

    public List<Genero> getGeneros() {
        return new ArrayList<>(generos);
    }

    public void agregarGenero(Genero genero) {
        if (!generos.contains(genero)) {
            generos.add(genero);
        }
    }

    // Muta la entidad cargada: otra con el mismo id pelearía por la fila en el contexto de persistencia.
    public void actualizar(String titulo, int duracionMinutos, List<Genero> generos,
                           Clasificacion clasificacion) {
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.clasificacion = clasificacion;
        this.generos.clear();
        generos.forEach(this::agregarGenero);
    }

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

    public boolean estaEnCartelera() {
        return enCartelera;
    }

    public void setEnCartelera(boolean enCartelera) {
        this.enCartelera = enCartelera;
    }

    public double getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(double puntaje) {
        this.puntaje = puntaje;
    }

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
