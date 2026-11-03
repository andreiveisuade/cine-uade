package ar.uade.cine.model.cartelera;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
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

import ar.uade.cine.model.cartelera.validacion.ValidadorPelicula;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.rechazos.DatoInvalido;

// Película del catálogo (R2, R7, R10, R31); Experto de revisión y formatos, validada por ValidadorPelicula.
@Entity
@Getter
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

    // TMDB no informa el formato: la que no dice otra cosa se estrena en 2D.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pelicula_formato",
            joinColumns = @JoinColumn(name = "pelicula_id"))
    @Column(name = "formato")
    @Enumerated(EnumType.STRING)
    private Set<Proyeccion> formatos = EnumSet.of(Proyeccion.DOS_D);

    @Embedded
    private CatalogoPelicula catalogo = new CatalogoPelicula();

    @Getter(AccessLevel.NONE)
    private boolean enCartelera = true;

    // Solo el id del video: la web lo embebe y Swing lo abre en el navegador. Null es "sin trailer".
    private String trailerYoutube;

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
    public void actualizar(String titulo, Integer duracionMinutos, List<Genero> generos,
                           Clasificacion clasificacion) {
        String tituloValido = ValidadorPelicula.titulo(titulo);
        int duracionValida = ValidadorPelicula.duracion(duracionMinutos);
        List<Genero> generosValidos = ValidadorPelicula.generos(generos);
        Clasificacion clasificacionValida = ValidadorPelicula.clasificacion(clasificacion);
        this.titulo = tituloValido;
        this.duracionMinutos = duracionValida;
        this.clasificacion = clasificacionValida;
        this.generos.clear();
        this.generos.addAll(generosValidos);
    }

    public Set<Proyeccion> getFormatos() {
        return EnumSet.copyOf(formatos);
    }

    public void cambiarFormatos(Collection<Proyeccion> formatos) {
        Set<Proyeccion> validos = ValidadorPelicula.formatos(formatos);
        this.formatos.clear();
        this.formatos.addAll(validos);
    }

    public boolean seEstrenaEn(Proyeccion proyeccion) {
        return formatos.contains(proyeccion);
    }

    // R31. Experto: la película sabe en qué formatos se estrena; la función y la grilla se lo preguntan.
    public void exigirFormato(Proyeccion proyeccion) {
        if (!seEstrenaEn(proyeccion)) {
            throw new DatoInvalido("La película " + titulo + " no tiene versión en " + proyeccion.getEtiqueta());
        }
    }

    // El catálogo es inmutable: se reemplaza entero por otro armado con conCambios, que ya viene validado.
    public void cambiarCatalogo(CatalogoPelicula catalogo) {
        this.catalogo = catalogo;
    }

    // Vacío lo saca; cualquier otra cosa tiene que ser un video de YouTube.
    public void cambiarTrailer(String texto) {
        trailerYoutube = texto.isBlank() ? null : new TrailerYoutube(texto).id();
    }

    // El que trae el importador solo llena el hueco: no pisa el que corrigió el encargado.
    public void completarTrailer(TrailerYoutube trailer) {
        if (trailerYoutube == null) {
            trailerYoutube = trailer.id();
        }
    }

    // El planificador de la grilla ordena por estos dos: se los pide a la película, no a su catálogo.
    public double getPuntaje() {
        return catalogo.puntaje();
    }

    public int getVotos() {
        return catalogo.votos();
    }

    public boolean estaEnCartelera() {
        return enCartelera;
    }

    // Es solo un veto: en cartelera está la que además tiene funciones por delante.
    // State: si se puede publicar lo decide su estado de revisión (ver EstadoRevision).
    public void ponerEnCartelera() {
        estadoRevision.exigirPublicable(titulo);
        enCartelera = true;
    }

    // State: si se puede programar lo decide su estado de revisión (ver EstadoRevision).
    public void exigirProgramable() {
        estadoRevision.exigirProgramable(titulo);
    }

    // State: si la ve quien no es del personal lo decide su estado de revisión (ver EstadoRevision).
    public boolean seMuestraAlPublico() {
        return estadoRevision.seMuestraAlPublico();
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

    @Override
    public String toString() {
        return "[" + id + "] " + titulo + " (" + duracionMinutos + " min) "
                + clasificacion.getEtiqueta() + " " + generos
                + (enCartelera ? "" : " - fuera de cartelera");
    }
}
