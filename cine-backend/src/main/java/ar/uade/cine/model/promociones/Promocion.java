package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;

/**
 * Descuento sobre el total de una reserva: un monto y no un factor por butaca, porque el 2x1
 * es una regla de grupo y las promos compiten entre sí (R15). Acá viven las condiciones
 * comunes; cada subclase solo dice cuánto descuenta. Tabla única con {@code tipo}.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String nombre;

    private LocalDate vigenciaDesde;

    private LocalDate vigenciaHasta;

    /** Vacío significa todos los días, no ninguno. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "promocion_dia", joinColumns = @JoinColumn(name = "promocion_id"))
    @Column(name = "dia")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> diasSemana = EnumSet.noneOf(DayOfWeek.class);

    /** {@code null} en cualquiera de los dos: todo el día. */
    private LocalTime horaDesde;

    private LocalTime horaHasta;

    /** Vacío es cualquier medio. Por esto el descuento se resuelve al cobrar y no al reservar. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "promocion_medio", joinColumns = @JoinColumn(name = "promocion_id"))
    @Column(name = "medio")
    @Enumerated(EnumType.STRING)
    private Set<MedioPago> mediosPago = EnumSet.noneOf(MedioPago.class);

    private boolean activa = true;

    protected Promocion() {
    }

    protected Promocion(String nombre, LocalDate vigenciaDesde, LocalDate vigenciaHasta,
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

    public abstract TipoPromocion getTipo();

    /** Recibe las entradas ya filtradas por R16, así ninguna subclase tiene que acordarse. */
    public abstract Dinero calcularDescuento(List<Entrada> entradas);

    /** Se evalúa contra el horario de la función, no de la compra: el 2x1 del miércoles vale comprando el lunes. */
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

    /** Un descuento mayor al total daría un cobro negativo. */
    protected static Dinero topear(Dinero descuento, List<Entrada> entradas) {
        return descuento.sinBajarDeCero().acotadoA(subtotalDe(entradas));
    }

    protected static Dinero subtotalDe(List<Entrada> entradas) {
        return Dinero.sumar(entradas.stream().map(Entrada::precio).toList());
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getVigenciaDesde() {
        return vigenciaDesde;
    }

    public LocalDate getVigenciaHasta() {
        return vigenciaHasta;
    }

    public Set<DayOfWeek> getDiasSemana() {
        return diasSemana.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(diasSemana);
    }

    public LocalTime getHoraDesde() {
        return horaDesde;
    }

    public LocalTime getHoraHasta() {
        return horaHasta;
    }

    public Set<MedioPago> getMediosPago() {
        return mediosPago.isEmpty() ? EnumSet.noneOf(MedioPago.class) : EnumSet.copyOf(mediosPago);
    }

    public boolean estaActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " (" + getTipo() + ")" + (activa ? "" : " - inactiva");
    }
}
