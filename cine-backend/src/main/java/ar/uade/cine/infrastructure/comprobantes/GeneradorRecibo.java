package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;

// Puerto: recibo de un cobro en efectivo; ComprobantesDeVentas no conoce el formato (Variaciones protegidas).
public interface GeneradorRecibo {

    void emitir(Pago pago, Reserva reserva);
}
