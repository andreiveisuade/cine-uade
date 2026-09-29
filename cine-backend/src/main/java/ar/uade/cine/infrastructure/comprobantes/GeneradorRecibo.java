package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;

// Puerto: el recibo de un cobro en efectivo; GestorPagos no conoce el formato (Variaciones protegidas).
public interface GeneradorRecibo {

    void emitir(Pago pago, Reserva reserva);
}
