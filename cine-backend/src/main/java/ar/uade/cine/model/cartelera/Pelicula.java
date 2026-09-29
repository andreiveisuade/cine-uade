package ar.uade.cine.model.cartelera;

import java.time.LocalDate;
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
import lombok.AccessLevel;
import lombok.Getter;

// Película del catálogo (R2, R7, R10); Experto: valida sus datos y decide su estado de revisión.
@Entity
@Getter
public class Pelicula {

    // La primera proyección pública, y un margen para las que se anuncian con años de anticipación.
    private static final int PRIMER_ANIO = 1895;
    private static final int ANIOS_POR_DELANTE = 5;

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

    @Getter(AccessLevel.NONE)
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
        actualizar(titulo, duracionMinutos, generos, clasificacion);
    }

    public List<Genero> getGeneros() {
        return new ArrayList<>(generos);
    }

    // El alta pasa por acá: alta y edición validan lo mismo, y todo antes de tocar un campo.
    // Muta la entidad cargada: otra con el mismo id pelearía por la fila en el contexto de persistencia.
    public void actualizar(String titulo, int duracionMinutos, List<Genero> generos,
                           Clasificacion clasificacion) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        exigirLargo(titulo, 100, "El título");
        if (duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a cero");
        }
        if (generos == null || generos.isEmpty()) {
            throw new IllegalArgumentException("La película necesita al menos un género");
        }
        if (clasificacion == null) {
            throw new IllegalArgumentException("Falta la clasificación por edad");
        }
        // Sin repetidos: la clave de pelicula_genero es (pelicula_id, genero).
        List<Genero> sinRepetir = generos.stream().distinct().toList();
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.clasificacion = clasificacion;
        this.generos.clear();
        this.generos.addAll(sinRepetir);
    }

    public void cambiarPuntaje(double puntaje) {
        if (puntaje < 0 || puntaje > 10) {
            throw new IllegalArgumentException("El puntaje va de 0 a 10");
        }
        this.puntaje = puntaje;
    }

    public void cambiarVotos(int votos) {
        if (votos < 0) {
            throw new IllegalArgumentException("Los votos no pueden ser negativos");
        }
        this.votos = votos;
    }

    public void cambiarDirector(String director) {
        exigirLargo(director, 100, "El director");
        this.director = director;
    }

    public void cambiarSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    // 0 es "sin dato", como lo deja el importador cuando TMDB no trae fecha de estreno. El día
    // de hoy lo pasa el gestor, que es el que tiene el reloj.
    public void cambiarAnio(int anio, LocalDate hoy) {
        int maximo = hoy.getYear() + ANIOS_POR_DELANTE;
        if (anio != 0 && (anio < PRIMER_ANIO || anio > maximo)) {
            throw new IllegalArgumentException("El año tiene que estar entre " + PRIMER_ANIO + " y " + maximo);
        }
        this.anio = anio;
    }

    public void cambiarIdiomaOriginal(String idiomaOriginal) {
        exigirLargo(idiomaOriginal, 40, "El idioma original");
        this.idiomaOriginal = idiomaOriginal;
    }

    public void cambiarPoster(String posterUrl) {
        exigirLargo(posterUrl, 255, "La URL del póster");
        this.posterUrl = posterUrl;
    }

    public boolean estaEnCartelera() {
        return enCartelera;
    }

    // Es solo un veto: en cartelera está la que además tiene funciones por delante.
    public void ponerEnCartelera() {
        enCartelera = true;
    }

    public void sacarDeCartelera() {
        enCartelera = false;
    }

    // Lo que trae el importador espera en el buzón: nadie la miró, así que no se ofrece.
    public void dejarPendiente() {
        estadoRevision = EstadoRevision.PENDIENTE;
        enCartelera = false;
    }

    // Confirmar es publicarla: levanta el veto que le puso el importador.
    public void confirmar() {
        estadoRevision = EstadoRevision.CONFIRMADA;
        enCartelera = true;
    }

    // Se guarda en vez de borrarse para que el importador no la vuelva a proponer.
    public void descartar() {
        estadoRevision = EstadoRevision.DESCARTADA;
        enCartelera = false;
    }

    // El largo de la columna de schema.sql: pasado, MySQL rechaza el INSERT y el usuario vería un 500.
    private static void exigirLargo(String texto, int maximo, String que) {
        if (texto.length() > maximo) {
            throw new IllegalArgumentException(que + " no puede tener más de " + maximo + " caracteres");
        }
    }

    @Override
    public String toString() {
        return "[" + id + "] " + titulo + " (" + duracionMinutos + " min) "
                + clasificacion.getEtiqueta() + " " + generos
                + (enCartelera ? "" : " - fuera de cartelera");
    }
}
