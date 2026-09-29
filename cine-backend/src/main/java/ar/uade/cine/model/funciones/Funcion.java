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
import lombok.AccessLevel;
import lombok.Getter;

// Pase de una película en una sala a una hora; Experto: valida formato, R8 y precio al nacer, y decide R19.
@Entity
@Getter
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

    @Column(name = "programacion_id")
    @Getter(AccessLevel.NONE)
    private Integer programacionId;

    private LocalDateTime inicio;

    @Enumerated(EnumType.STRING)
    private Version version;

    @Enumerated(EnumType.STRING)
    private Proyeccion proyeccion;

    private Dinero precio;

    protected Funcion() {
    }

    public Funcion(Pelicula pelicula, Sala sala, LocalDateTime inicio, Version version,
                   Proyeccion proyeccion, Dinero precio) {
        this(pelicula, sala, inicio, version, proyeccion, precio, null);
    }

    // Que la película esté confirmada, que no empiece en el pasado (R20) y que no pise a otra (R3)
    // no son invariantes de la función sino del alta: dependen del buzón, del reloj y de las otras
    // funciones de la sala. Una función que quedó en el pasado sigue siendo válida (R12).
    public Funcion(Pelicula pelicula, Sala sala, LocalDateTime inicio, Version version,
                   Proyeccion proyeccion, Dinero precio, Integer programacionId) {
        validarProgramable(sala, version, proyeccion, precio);
        if (inicio == null) {
            throw new IllegalArgumentException("Falta la fecha y hora de la función");
        }
        this.pelicula = pelicula;
        this.sala = sala;
        this.inicio = inicio;
        this.version = version;
        this.proyeccion = proyeccion;
        this.precio = precio;
        this.programacionId = programacionId;
    }

    // Pública porque una programación genera funciones con estos mismos datos: tiene que
    // rechazarlos antes de generar la primera, y con el mismo mensaje que el alta de una suelta.
    public static void validarProgramable(Sala sala, Version version, Proyeccion proyeccion, Dinero precio) {
        if (version == null || proyeccion == null) {
            throw new IllegalArgumentException("Falta la versión o el formato de proyección");
        }
        // R8
        if (proyeccion == Proyeccion.TRES_D && !sala.getTipo().soportaTresD()) {
            throw new IllegalArgumentException("La sala " + sala.getNombre() + " no puede proyectar en 3D");
        }
        Dinero.importeValido(precio, "precio");
    }

    // No inicializa el proxy: sirve fuera de la transacción, donde se arman las vistas.
    public int getPeliculaId() {
        return pelicula.getId();
    }

    public int getSalaId() {
        return sala.getId();
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + getPeliculaId() + " en sala " + getSalaId() + " - " + inicio
                + " - " + proyeccion + " " + version + " - desde $" + precio;
    }

    public boolean yaEmpezo(LocalDateTime ahora) {
        return !inicio.isAfter(ahora);
    }

    public LocalDateTime getFin(int duracionMinutos) {
        return inicio.plusMinutes(duracionMinutos);
    }
}
