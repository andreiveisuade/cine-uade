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
 * Un descuento sobre el total de una reserva. Es un monto sobre el conjunto y no un
 * factor por butaca porque el 2x1 es una regla sobre el grupo, y porque las promociones
 * compiten entre sí (R15) y tienen que producir la misma unidad.
 *
 * <p>Abstracta y no un {@code switch}: hay tres implementaciones de verdad y sumar un
 * beneficio nuevo es una clase, no un {@code case}. Acá viven las condiciones que las tres
 * comparten —vigencia, día, horario, medio—; cada subclase solo dice cuánto descuenta.
 * Van a la misma tabla con {@code tipo} como discriminador; las columnas del beneficio
 * quedan NULL en las clases que no las usan.
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

    /** {@code null} en cualquiera de los dos significa que corre todo el día. */
    private LocalTime horaDesde;

    private LocalTime horaHasta;

    /** Vacío significa cualquier medio. Es lo que obliga a resolver el descuento al cobrar y no al reservar. */
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

    /** Discriminador de la tabla única: qué clase es esta fila. */
    public abstract TipoPromocion getTipo();

    /**
     * Cuánto descuenta sobre esas entradas. Recibe la lista ya filtrada por R16 (sin las
     * tarifas reducidas) para que ninguna subclase tenga que acordarse de esa regla.
     */
    public abstract Dinero calcularDescuento(List<Entrada> entradas);

    /**
     * Si corre para esa función pagada con ese medio. Se evalúa contra el horario de la
     * <strong>función</strong>, no de la compra: el 2x1 del miércoles es para la función
     * del miércoles aunque se compre el lunes. Un conjunto vacío no restringe.
     */
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
    protected static Dinero topear(Dinero descuento, List<Entrada> entradas) {
        return descuento.sinBajarDeCero().acotadoA(subtotalDe(entradas));
    }

    /** Lo que suman las entradas que participan del descuento. */
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
