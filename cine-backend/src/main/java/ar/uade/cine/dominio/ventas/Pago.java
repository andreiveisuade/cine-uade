package ar.uade.cine.dominio.ventas;

import java.time.LocalDateTime;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Comprobante de cobro de una reserva. Tiene entidad propia —y no es un campo más de
 * la reserva— porque se consulta por sí mismo: para un arqueo hay que poder listar los
 * pagos de un día sin pasar por las reservas.
 */
public interface Pago {

    int getId();

    void setId(int id);

    int getReservaId();

    /** No se pasa por parámetro: se toma del total de la reserva, así nadie cobra otra cosa. */
    /** Suma de los precios de lista de las entradas, antes de cualquier descuento. */
    Dinero getSubtotal();

    /**
     * La promoción que ganó, o {@code null} si no aplicó ninguna. Guardarla además del
     * monto es lo que permite explicar meses después por qué se cobró eso, aunque para
     * entonces la promoción ya esté desactivada.
     */
    Integer getPromocionId();

    /** Cuánto sacó la promoción. Cero si no hubo. */
    Dinero getDescuento();

    /** Lo que entró de verdad en la caja: subtotal menos descuento. Es lo que suma el arqueo. */
    Dinero getMonto();

    MedioPago getMedio();

    LocalDateTime getFecha();

    /** Código que devuelve el procesador. Vacío cuando se pagó en efectivo. */
    String getCodigoAutorizacion();
}
