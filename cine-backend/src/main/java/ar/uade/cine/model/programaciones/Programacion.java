package ar.uade.cine.model.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
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
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;

// Película repetida en una sala a una hora por un rango; Experto: valida su rango y sabe qué falta generar.
@Entity
@Getter
public class Programacion {

    private static final int HORIZONTE_DIAS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pelicula_id", nullable = false)
    private Pelicula pelicula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    private LocalDate desde;

    private LocalDate hasta;

    private LocalTime horaInicio;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "programacion_dia",
            joinColumns = @JoinColumn(name = "programacion_id"))
    @Column(name = "dia")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> diasSemana = EnumSet.noneOf(DayOfWeek.class);

    @Enumerated(EnumType.STRING)
    private Version version;

    @Enumerated(EnumType.STRING)
    private Proyeccion proyeccion;

    private Dinero precio;

    @Getter(AccessLevel.NONE)
    private boolean activa = true;

    // Se guarda y no se deriva de la última función: si esa se borra o mueve, se regenerarían fechas.
    private LocalDate generadaHasta;

    protected Programacion() {
    }

    // La película, la sala, el formato y el precio los valida el gestor, después de estos y con el
    // 404 primero: acá llegan como referencias sin cargar, y leerlas iría a la base.
    public Programacion(Pelicula pelicula, Sala sala, LocalDate desde, LocalDate hasta,
                        LocalTime horaInicio, Set<DayOfWeek> diasSemana, Version version,
                        Proyeccion proyeccion, Dinero precio) {
        if (desde == null) {
            throw new IllegalArgumentException("Falta la fecha de inicio");
        }
        if (hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango tiene que empezar antes de terminar");
        }
        if (horaInicio == null) {
            throw new IllegalArgumentException("Falta la hora de la función");
        }
        this.pelicula = pelicula;
        this.sala = sala;
        this.desde = desde;
        this.hasta = hasta;
        this.horaInicio = horaInicio;
        this.diasSemana = diasSemana == null || diasSemana.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(diasSemana);
        this.version = version;
        this.proyeccion = proyeccion;
        this.precio = precio;
        if (hasta != null && horarios(hasta).isEmpty()) {
            throw new IllegalArgumentException(
                    "Ningún día del rango cae en los días elegidos: la grilla no generaría funciones");
        }
    }

    public List<LocalDateTime> horarios(LocalDate tope) {
        LocalDate fin = hasta == null || tope.isBefore(hasta) ? tope : hasta;
        List<LocalDateTime> momentos = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            if (diasSemana.isEmpty() || diasSemana.contains(dia.getDayOfWeek())) {
                momentos.add(LocalDateTime.of(dia, horaInicio));
            }
        }
        return momentos;
    }

    public LocalDate topePara(LocalDate hoy) {
        return hasta != null ? hasta : hoy.plusDays(HORIZONTE_DIAS);
    }

    public boolean estaAlDia(LocalDate hoy) {
        return generadaHasta != null && !generadaHasta.isBefore(topePara(hoy));
    }

    // Por fecha procesada y no por función existente: una que chocó se reintentaría siempre.
    public List<LocalDateTime> horariosSinGenerar(LocalDate tope) {
        return horarios(tope).stream()
                .filter(inicio -> generadaHasta == null || inicio.toLocalDate().isAfter(generadaHasta))
                .toList();
    }

    // El tope y no la última generada: las que chocaron también quedan procesadas.
    public void marcarGeneradaHasta(LocalDate tope) {
        this.generadaHasta = tope;
    }

    // No inicializa el proxy: sirve fuera de la transacción, donde se arman las vistas.
    public int getPeliculaId() {
        return pelicula.getId();
    }

    public int getSalaId() {
        return sala.getId();
    }

    public Set<DayOfWeek> getDiasSemana() {
        return Collections.unmodifiableSet(
                diasSemana.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(diasSemana));
    }

    public boolean estaActiva() {
        return activa;
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + getPeliculaId() + " en sala " + getSalaId() + " - " + horaInicio
                + " del " + desde + (hasta == null ? " en adelante" : " al " + hasta)
                + " - generada hasta " + (generadaHasta == null ? "nunca" : generadaHasta);
    }
}
