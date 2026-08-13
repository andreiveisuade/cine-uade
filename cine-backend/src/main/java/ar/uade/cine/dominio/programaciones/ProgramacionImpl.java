package ar.uade.cine.dominio.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;

/**
 * Referencia película y sala por id: el DAO trae el objeto completo cuando hace falta.
 */
public class ProgramacionImpl implements Programacion {

    private int id;
    private final int peliculaId;
    private final int salaId;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final LocalTime horaInicio;
    private final Set<DayOfWeek> diasSemana;
    private final Version version;
    private final Proyeccion proyeccion;
    private final double precio;
    private boolean activa = true;

    public ProgramacionImpl(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                            LocalTime horaInicio, Set<DayOfWeek> diasSemana, Version version,
                            Proyeccion proyeccion, double precio) {
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

    public ProgramacionImpl(int id, int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                            LocalTime horaInicio, Set<DayOfWeek> diasSemana, Version version,
                            Proyeccion proyeccion, double precio) {
        this(peliculaId, salaId, desde, hasta, horaInicio, diasSemana, version, proyeccion, precio);
        this.id = id;
    }

    /**
     * Recorre el rango día por día y se queda con los que la grilla habilita. Vive acá y
     * no en el gestor porque no consulta nada ni decide nada del negocio: es cómo se lee
     * el patrón temporal que la propia programación guarda.
     */
    @Override
    public List<LocalDateTime> horarios() {
        List<LocalDateTime> momentos = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            if (diasSemana.isEmpty() || diasSemana.contains(dia.getDayOfWeek())) {
                momentos.add(LocalDateTime.of(dia, horaInicio));
            }
        }
        return momentos;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getPeliculaId() {
        return peliculaId;
    }

    @Override
    public int getSalaId() {
        return salaId;
    }

    @Override
    public LocalDate getDesde() {
        return desde;
    }

    @Override
    public LocalDate getHasta() {
        return hasta;
    }

    @Override
    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    @Override
    public Set<DayOfWeek> getDiasSemana() {
        return diasSemana;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public Proyeccion getProyeccion() {
        return proyeccion;
    }

    @Override
    public double getPrecio() {
        return precio;
    }

    @Override
    public boolean estaActiva() {
        return activa;
    }

    @Override
    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + peliculaId + " en sala " + salaId + " - " + horaInicio
                + " del " + desde + " al " + hasta + " - " + horarios().size() + " funciones";
    }
}
