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

import ar.uade.cine.model.rechazos.DatoInvalido;

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

    public Pelicula(String titulo, Integer duracionMinutos, List<Genero> generos,
                    Clasificacion clasificacion) {
        actualizar(titulo, duracionMinutos, generos, clasificacion);
    }

    public List<Genero> getGeneros() {
        return new ArrayList<>(generos);
    }

    // El alta pasa por acá: alta y edición validan lo mismo, y todo antes de tocar un campo.
    // Muta la entidad cargada: otra con el mismo id pelearía por la fila en el contexto de persistencia.
    // La duración llega como Integer porque en el alta viene del pedido: la que no vino falta, no es cero.
    public void actualizar(String titulo, Integer duracionMinutos, List<Genero> generos,
                           Clasificacion clasificacion) {
        String tituloLimpio = tituloValido(titulo);
        if (duracionMinutos == null) {
            throw new DatoInvalido("Falta la duración");
        }
        if (duracionMinutos <= 0) {
            throw new DatoInvalido("La duración tiene que ser mayor a cero");
        }
        if (generos == null || generos.isEmpty()) {
            throw new DatoInvalido("La película necesita al menos un género");
        }
        if (clasificacion == null) {
            throw new DatoInvalido("Falta la clasificación por edad");
        }
        // Sin repetidos: la clave de pelicula_genero es (pelicula_id, genero).
        List<Genero> sinRepetir = generos.stream().distinct().toList();
        this.titulo = tituloLimpio;
        this.duracionMinutos = duracionMinutos;
        this.clasificacion = clasificacion;
        this.generos.clear();
        this.generos.addAll(sinRepetir);
    }

    public void cambiarPuntaje(double puntaje) {
        if (puntaje < 0 || puntaje > 10) {
            throw new DatoInvalido("El puntaje tiene que estar entre 0 y 10");
        }
        this.puntaje = puntaje;
    }

    public void cambiarVotos(int votos) {
        if (votos < 0) {
            throw new DatoInvalido("Los votos no pueden ser negativos");
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
            throw new DatoInvalido("El año tiene que estar entre " + PRIMER_ANIO + " y " + maximo);
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

    public boolean estaConfirmada() {
        return estadoRevision == EstadoRevision.CONFIRMADA;
    }

    // Es solo un veto: en cartelera está la que además tiene funciones por delante. Una pendiente
    // o descartada no se publica: si no, el botón Publicar saltearía el buzón de revisión.
    public void ponerEnCartelera() {
        if (!estaConfirmada()) {
            throw new DatoInvalido("La película " + titulo
                    + " no está confirmada: revisala antes de publicarla");
        }
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

    // Recortado: " Matrix" pasaría el chequeo de título repetido de R1 como si fuera otra película.
    private static String tituloValido(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new DatoInvalido("El título no puede estar vacío");
        }
        String limpio = titulo.strip();
        exigirLargo(limpio, 100, "El título");
        return limpio;
    }

    // El largo de la columna de schema.sql: pasado, MySQL rechaza el INSERT y el usuario vería un 500.
    private static void exigirLargo(String texto, int maximo, String que) {
        if (texto.length() > maximo) {
            throw new DatoInvalido(que + " no puede tener más de " + maximo + " caracteres");
        }
    }

    @Override
    public String toString() {
        return "[" + id + "] " + titulo + " (" + duracionMinutos + " min) "
                + clasificacion.getEtiqueta() + " " + generos
                + (enCartelera ? "" : " - fuera de cartelera");
    }
}
