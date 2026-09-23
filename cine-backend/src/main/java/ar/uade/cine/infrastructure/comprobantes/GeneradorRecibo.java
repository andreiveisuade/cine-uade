package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;

/**
 * Emite el recibo de caja, solo para pagos en efectivo (R11 al revés): el electrónico ya
 * deja su código de autorización en el {@link Pago}, el efectivo no deja constancia. El
 * criterio es {@code medio.requiereAutorizacion()}, no una lista de medios.
 */
public interface GeneradorRecibo {

    void emitir(Pago pago, Reserva reserva);
}
