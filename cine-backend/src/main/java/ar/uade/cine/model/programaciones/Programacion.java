package ar.uade.cine.model.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
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
public class Programacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "pelicula_id")
    private int peliculaId;

    @Column(name = "sala_id")
    private int salaId;

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

    private boolean activa = true;

    private LocalDate generadaHasta;

    protected Programacion() {
    }

    public Programacion(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                        LocalTime horaInicio, Set<DayOfWeek> diasSemana, Version version,
                        Proyeccion proyeccion, Dinero precio) {
        this.peliculaId = peliculaId;
        this.salaId = salaId;
        this.desde = desde;
        this.hasta = hasta;
        this.horaInicio = horaInicio;
        this.diasSemana = diasSemana == null || diasSemana.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(diasSemana);
        this.version = version;
        this.proyeccion = proyeccion;
        this.precio = precio;
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

    // Se guarda y no se deriva de la última función: si esa se borra o mueve, se regenerarían fechas.
    public LocalDate getGeneradaHasta() {
        return generadaHasta;
    }

    public void setGeneradaHasta(LocalDate generadaHasta) {
        this.generadaHasta = generadaHasta;
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

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public Set<DayOfWeek> getDiasSemana() {
        return diasSemana;
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

    public boolean estaActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + peliculaId + " en sala " + salaId + " - " + horaInicio
                + " del " + desde + (hasta == null ? " en adelante" : " al " + hasta)
                + " - generada hasta " + (generadaHasta == null ? "nunca" : generadaHasta);
    }
}
