package ar.uade.cine.model.funciones;

import java.time.LocalDateTime;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Una película en una sala a una fecha y hora. Referencia película y sala por id y no con
 * {@code @ManyToOne}: son vecinos, no partes, y cargarlos en cada función sobra. Las
 * relaciones se mapean solo donde una cosa no existe sin la otra (entradas de una reserva).
 */
@Entity
public class Funcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "pelicula_id")
    private int peliculaId;

    @Column(name = "sala_id")
    private int salaId;

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
    public Funcion(int peliculaId, int salaId, LocalDateTime inicio, Version version,
                   Proyeccion proyeccion, Dinero precio) {
        this(peliculaId, salaId, inicio, version, proyeccion, precio, null);
    }

    public Funcion(int peliculaId, int salaId, LocalDateTime inicio, Version version,
                   Proyeccion proyeccion, Dinero precio, Integer programacionId) {
        this.peliculaId = peliculaId;
        this.salaId = salaId;
        this.inicio = inicio;
        this.version = version;
        this.proyeccion = proyeccion;
        this.precio = precio;
        this.programacionId = programacionId;
    }

    public int getId() {
        return id;
    }

    public int getPeliculaId() {
        return peliculaId;
    }

    public int getSalaId() {
        return salaId;
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
        return "[" + id + "] película " + peliculaId + " en sala " + salaId + " - " + inicio
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
