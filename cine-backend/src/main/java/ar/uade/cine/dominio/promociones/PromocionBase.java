package ar.uade.cine.dominio.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.MedioPago;

/**
 * Las condiciones que comparten las tres promociones: cuándo corre y con qué se paga.
 * Solo el cálculo del descuento queda abierto, que es lo único que las diferencia.
 *
 * <p>Existe para no repetir {@code aplicaA()} tres veces. Sin ella, cada implementación
 * tendría su propia copia de la evaluación de vigencia, día, horario y medio, y las tres
 * copias se irían separando con el tiempo.
 */
public abstract class PromocionBase implements Promocion {

    private int id;
    private final String nombre;
    private final LocalDate vigenciaDesde;
    private final LocalDate vigenciaHasta;
    private final Set<DayOfWeek> diasSemana;
    private final LocalTime horaDesde;
    private final LocalTime horaHasta;
    private final Set<MedioPago> mediosPago;
    private boolean activa = true;

    protected PromocionBase(String nombre, LocalDate vigenciaDesde, LocalDate vigenciaHasta,
                            Set<DayOfWeek> diasSemana, LocalTime horaDesde, LocalTime horaHasta,
                            Set<MedioPago> mediosPago) {
        this.nombre = nombre;
        this.vigenciaDesde = vigenciaDesde;
        this.vigenciaHasta = vigenciaHasta;
        this.diasSemana = diasSemana == null || diasSemana.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(diasSemana);
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
        this.mediosPago = mediosPago == null || mediosPago.isEmpty()
                ? EnumSet.noneOf(MedioPago.class) : EnumSet.copyOf(mediosPago);
    }

    /**
     * Un conjunto vacío no restringe: quiere decir "todos los días" o "cualquier medio",
     * y no "ninguno". Es lo mismo que en la base, donde la promoción sin filas en
     * promocion_dia corre siempre.
     */
    @Override
    public boolean aplicaA(LocalDateTime inicioFuncion, MedioPago medio) {
        if (!activa || inicioFuncion == null) {
            return false;
        }
        LocalDate dia = inicioFuncion.toLocalDate();
        if (dia.isBefore(vigenciaDesde) || dia.isAfter(vigenciaHasta)) {
            return false;
        }
        if (!diasSemana.isEmpty() && !diasSemana.contains(dia.getDayOfWeek())) {
            return false;
        }
        if (!mediosPago.isEmpty() && !mediosPago.contains(medio)) {
            return false;
        }
        LocalTime hora = inicioFuncion.toLocalTime();
        if (horaDesde != null && hora.isBefore(horaDesde)) {
            return false;
        }
        return horaHasta == null || !hora.isAfter(horaHasta);
    }

    /** Nunca descuenta más que el total: un descuento mayor daría un cobro negativo. */
    protected static double topear(double descuento, List<Entrada> entradas) {
        double total = entradas.stream().mapToDouble(Entrada::precio).sum();
        return Math.min(Math.max(descuento, 0), total);
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
    public String getNombre() {
        return nombre;
    }

    @Override
    public LocalDate getVigenciaDesde() {
        return vigenciaDesde;
    }

    @Override
    public LocalDate getVigenciaHasta() {
        return vigenciaHasta;
    }

    @Override
    public Set<DayOfWeek> getDiasSemana() {
        return EnumSet.copyOf(diasSemana.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : diasSemana);
    }

    @Override
    public LocalTime getHoraDesde() {
        return horaDesde;
    }

    @Override
    public LocalTime getHoraHasta() {
        return horaHasta;
    }

    @Override
    public Set<MedioPago> getMediosPago() {
        return mediosPago.isEmpty() ? EnumSet.noneOf(MedioPago.class) : EnumSet.copyOf(mediosPago);
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
        return "[" + id + "] " + nombre + " (" + getTipo() + ")" + (activa ? "" : " - inactiva");
    }
}
