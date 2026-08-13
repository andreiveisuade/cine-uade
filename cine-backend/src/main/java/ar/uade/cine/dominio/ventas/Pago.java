package ar.uade.cine.dominio.ventas;

import java.time.LocalDateTime;

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
    double getMonto();

    MedioPago getMedio();

    LocalDateTime getFecha();

    /** Código que devuelve el procesador. Vacío cuando se pagó en efectivo. */
    String getCodigoAutorizacion();
}
