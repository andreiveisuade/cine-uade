package ar.uade.cine.dominio.candy;

import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.dominio.ventas.MedioPago;

/**
 * Una venta del candy. A diferencia de la reserva de butacas, no hay estados: en el
 * mostrador se paga en el acto, así que la compra ya nace cobrada y lleva encima con
 * qué se pagó. Por eso tampoco pasa por Pago, que existe para el circuito de reservar
 * primero y cobrar después.
 */
public interface CompraCandy {

    int getId();

    void setId(int id);

    /**
     * Quién compró, o {@code null} si la venta fue de mostrador y nadie se identificó:
     * pedir el nombre para vender pochoclos no tiene sentido.
     */
    Integer getClienteId();

    /**
     * La reserva a la que se le agregó esta compra, o {@code null} si fue de mostrador.
     * Es el <em>«¿desea agregar pochoclos?»</em> que aparece después de comprar la
     * entrada por la web: de ahí sale el cliente sin volver a pedírselo, permite retirar
     * mostrando el mismo QR de la entrada, y le da al arqueo cuánto vende el upsell
     * contra el mostrador.
     */
    Integer getReservaId();

    LocalDateTime getFecha();

    MedioPago getMedio();

    /** Código del procesador. Vacío cuando se pagó en efectivo. */
    String getCodigoAutorizacion();

    /**
     * Qué se llevó. Se fijan al vender y no cambian: en el mostrador se paga en el acto,
     * así que la compra nace cerrada.
     */
    List<ItemCompra> getItems();

    /** Derivado de los items: no se guarda por separado. */
    double getTotal();
}
