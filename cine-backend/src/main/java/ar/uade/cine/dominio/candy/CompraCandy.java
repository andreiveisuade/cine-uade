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

    int getClienteId();

    LocalDateTime getFecha();

    MedioPago getMedio();

    /** Código del procesador. Vacío cuando se pagó en efectivo. */
    String getCodigoAutorizacion();

    List<ItemCompra> getItems();

    /** Lo usa el DAO al reconstruir la compra desde la base. */
    void agregarItem(ItemCompra item);

    /** Derivado de los items: no se guarda por separado. */
    double getTotal();
}
