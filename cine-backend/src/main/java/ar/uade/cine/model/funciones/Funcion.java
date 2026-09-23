package ar.uade.cine.model.funciones;

import java.time.LocalDateTime;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.salas.Sala;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Una película en una sala a una fecha y hora. Película y sala son LAZY: listar funciones
 * no tiene que traer ambas por fila, y los ids se leen del proxy sin ir a la base.
 */
@Entity
public class Funcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pelicula_id", nullable = false)
    private Pelicula pelicula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    /** {@code null} si se cargó a mano (preestreno, función especial). */
    @Column(name = "programacion_id")
    private Integer programacionId;

    private LocalDateTime inicio;

    @Enumerated(EnumType.STRING)
    private Version version;

    @Enumerated(EnumType.STRING)
    private Proyeccion proyeccion;

    /** Precio de una butaca estándar; los recargos de sala y butaca se aplican aparte. */
    private Dinero precio;

    protected Funcion() {
    }

    /** Función suelta de CU-03, sin grilla. */
    public Funcion(Pelicula pelicula, Sala sala, LocalDateTime inicio, Version version,
                   Proyeccion proyeccion, Dinero precio) {
        this(pelicula, sala, inicio, version, proyeccion, precio, null);
    }

    public Funcion(Pelicula pelicula, Sala sala, LocalDateTime inicio, Version version,
                   Proyeccion proyeccion, Dinero precio, Integer programacionId) {
        this.pelicula = pelicula;
        this.sala = sala;
        this.inicio = inicio;
        this.version = version;
        this.proyeccion = proyeccion;
        this.precio = precio;
        this.programacionId = programacionId;
    }

    public int getId() {
        return id;
    }

    public Pelicula getPelicula() {
        return pelicula;
    }

    public Sala getSala() {
        return sala;
    }

    /** No inicializa el proxy: sirve fuera de la transacción, donde se arman las vistas. */
    public int getPeliculaId() {
        return pelicula.getId();
    }

    public int getSalaId() {
        return sala.getId();
    }

    public Integer getProgramacionId() {
        return programacionId;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public Version getVersion() {
        return version;
    }

    public Proyeccion getProyeccion() {
        return proyeccion;
    }

    public Dinero getPrecio() {
        return precio;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + getPeliculaId() + " en sala " + getSalaId() + " - " + inicio
                + " - " + proyeccion + " " + version + " - desde $" + precio;
    }

    /** R19. Recibe el instante en vez de leer el reloj para poder probarlo. */
    public boolean yaEmpezo(LocalDateTime ahora) {
        return !inicio.isAfter(ahora);
    }

    /** La duración es de la película: la pasa quien ya la resolvió, en vez de copiarla acá. */
    public LocalDateTime getFin(int duracionMinutos) {
        return inicio.plusMinutes(duracionMinutos);
    }
}
