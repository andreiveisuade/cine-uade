package ar.uade.cine.dominio.ventas;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Butacas de una función a nombre de un cliente. Nace {@link EstadoReserva#RESERVADA}
 * y desde ahí solo avanza: a PAGADA cuando se cobra, o a CANCELADA cuando se libera sin
 * cobrar. No hay vuelta atrás entre esos dos estados finales.
 */
public interface Reserva {

    /**
     * Minutos que una reserva sin pagar retiene sus butacas. Es una regla de negocio, no
     * un detalle de implementación: en un cine una reserva abandonada no puede dejar una
     * función sin lugares que en realidad nadie compró.
     */
    int MINUTOS_PARA_PAGAR = 30;

    int getId();

    void setId(int id);

    int getFuncionId();

    int getClienteId();

    /** Cuándo se hizo. Sin esto no se puede ordenar el historial ni auditar una venta. */
    LocalDateTime getCreadaEn();

    /**
     * Una entrada por butaca elegida. Se fijan al crear la reserva y no se tocan más: una
     * butaca de menos o de más cambiaría el total de algo que ya se cobró.
     */
    List<Entrada> getEntradas();

    /** Derivada de las entradas: no se guarda por separado. */
    int getCantidadEntradas();

    /**
     * Suma de los precios de lista de las butacas. Es un <em>subtotal</em>: el total
     * definitivo aparece recién al cobrar, cuando se sabe el medio de pago y con él qué
     * promoción aplica.
     */
    double getTotal();

    EstadoReserva getEstado();

    /** Pasa de RESERVADA a PAGADA, CANCELADA o EXPIRADA. */
    void setEstado(EstadoReserva estado);

    /**
     * Si sigue reteniendo sus butacas. Lo son la que está esperando pago y la ya cobrada;
     * no lo son la cancelada ni la expirada.
     *
     * <p>Existe para no repetir la doble negación {@code estado != CANCELADA} en cada
     * consulta, que además se volvió incorrecta al aparecer EXPIRADA.
     */
    boolean estaVigente();

    /**
     * Si está esperando pago desde hace más de {@link #MINUTOS_PARA_PAGAR}. Recibe el
     * instante por parámetro y no lo pide al reloj para que se pueda probar sin esperar
     * media hora.
     *
     * <p>Vencida no es lo mismo que EXPIRADA: esto dice que <em>debería</em> expirar. El
     * estado lo escribe la primera operación que se cruza con ella.
     */
    boolean estaVencida(LocalDateTime ahora);

    // ---------- control de acceso ----------

    /**
     * El código que va en el QR de la entrada. Como el cliente no inicia sesión, es la
     * única credencial que existe en el sistema: por eso no puede ser el id, o con la
     * reserva 5 en la mano se imprime la 6.
     */
    String getCodigo();

    /** Cuándo entraron al cine, o {@code null} si todavía no lo hicieron. */
    LocalDateTime getIngresadaEn();

    /** Lo usa el DAO al reconstruir la reserva desde la base. */
    void setIngresadaEn(LocalDateTime ingresadaEn);
}
