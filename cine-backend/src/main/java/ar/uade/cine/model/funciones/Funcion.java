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
