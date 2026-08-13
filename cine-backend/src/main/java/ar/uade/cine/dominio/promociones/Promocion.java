package ar.uade.cine.dominio.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.MedioPago;

/**
 * Un descuento sobre el total de una reserva.
 *
 * <p><strong>Por qué el descuento es un monto sobre el conjunto y no un factor por
 * butaca:</strong> el 2x1 —"cada dos entradas, una gratis"— es una regla sobre el grupo,
 * y no existe multiplicador por butaca que lo exprese. Como además las promociones no se
 * acumulan y gana la que más descuenta (R15), todas tienen que ser comparables entre sí,
 * o sea producir la misma unidad. Esa unidad es un monto en pesos.
 *
 * <p>Es interfaz y no clase porque hay tres implementaciones de verdad, que es cuando la
 * abstracción paga: sumar un tipo de beneficio nuevo es una clase, no un {@code case}
 * más adentro de un switch que hay que ir a buscar.
 */
public interface Promocion {

    int getId();

    void setId(int id);

    String getNombre();

    /** Discriminador para la tabla única: qué implementación reconstruir al leer. */
    TipoPromocion getTipo();

    // ---------- condiciones ----------

    LocalDate getVigenciaDesde();

    LocalDate getVigenciaHasta();

    /** Vacío significa todos los días, no ninguno. */
    Set<DayOfWeek> getDiasSemana();

    /** {@code null} en cualquiera de los dos significa que corre todo el día. */
    LocalTime getHoraDesde();

    LocalTime getHoraHasta();

    /**
     * Vacío significa con cualquier medio. Esto es lo que obliga a que el descuento se
     * resuelva al cobrar y no al reservar: el medio de pago se elige recién ahí.
     */
    Set<MedioPago> getMediosPago();

    boolean estaActiva();

    void setActiva(boolean activa);

    /**
     * Si corre para esa función pagada con ese medio.
     *
     * <p>Todo se evalúa contra el horario de la <strong>función</strong> y no contra el
     * momento de la compra: un 2x1 de los miércoles es para la función del miércoles,
     * aunque las entradas se compren el lunes.
     */
    boolean aplicaA(LocalDateTime inicioFuncion, MedioPago medio);

    /**
     * Cuánto descuenta sobre esas entradas.
     *
     * <p>Recibe una lista de entradas y no la reserva entera por R16: las de tarifa
     * reducida no participan del descuento, así que el gestor le pasa solo las que
     * corresponden. Si la regla viviera adentro, habría que acordarse de repetirla en
     * cada implementación.
     */
    double calcularDescuento(List<Entrada> entradas);
}
