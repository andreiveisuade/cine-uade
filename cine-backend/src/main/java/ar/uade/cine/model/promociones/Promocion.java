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
 * Un descuento sobre el total de una reserva.
 *
 * <p><strong>Por qué el descuento es un monto sobre el conjunto y no un factor por
 * butaca:</strong> el 2x1 —"cada dos entradas, una gratis"— es una regla sobre el grupo, y
 * no existe multiplicador por butaca que lo exprese. Como además las promociones no se
 * acumulan y gana la que más descuenta (R15), todas tienen que ser comparables entre sí, o
 * sea producir la misma unidad. Esa unidad es un monto en pesos.
 *
 * <p>Es abstracta y no una clase con un {@code switch} porque hay tres implementaciones de
 * verdad, que es cuando la abstracción paga: sumar un tipo de beneficio nuevo es una clase,
 * no un {@code case} más adentro de un switch que hay que ir a buscar. Es la única jerarquía
 * del dominio que sobrevivió al pasaje a JPA con la forma que tenía —el resto eran interfaces
 * con un solo implementador— y acá el polimorfismo es real: cada subclase calcula distinto.
 *
 * <p>Las tres van a la misma tabla con {@code tipo} como discriminador, que es lo que ya
 * hacía el schema. Las columnas del beneficio —porcentaje, monto, lleva, paga— son NULL en
 * las clases que no las usan: con tres tablas separadas se ganaba normalización y se perdía
 * poder leer una promoción entera de un SELECT.
 *
 * <p>Acá viven además las condiciones que comparten las tres: cuándo corre y con qué se
 * paga. Sin eso, cada implementación tendría su copia de la evaluación de vigencia, día,
 * horario y medio, y las tres copias se irían separando con el tiempo.
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

    /**
     * Vacío significa con cualquier medio. Esto es lo que obliga a que el descuento se
     * resuelva al cobrar y no al reservar: el medio de pago se elige recién ahí.
     */
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
     * Cuánto descuenta sobre esas entradas.
     *
     * <p>Recibe una lista de entradas y no la reserva entera por R16: las de tarifa reducida
     * no participan del descuento, así que el gestor le pasa solo las que corresponden. Si la
     * regla viviera adentro, habría que acordarse de repetirla en cada implementación.
     */
    public abstract Dinero calcularDescuento(List<Entrada> entradas);

    /**
     * Si corre para esa función pagada con ese medio.
     *
     * <p>Todo se evalúa contra el horario de la <strong>función</strong> y no contra el
     * momento de la compra: un 2x1 de los miércoles es para la función del miércoles, aunque
     * las entradas se compren el lunes.
     *
     * <p>Un conjunto vacío no restringe: quiere decir "todos los días" o "cualquier medio", y
     * no "ninguno". Es lo mismo que en la base, donde la promoción sin filas en
     * promocion_dia corre siempre.
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

    public void setId(int id) {
        this.id = id;
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
