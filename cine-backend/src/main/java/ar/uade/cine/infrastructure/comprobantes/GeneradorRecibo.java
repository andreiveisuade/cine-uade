package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;

public interface GeneradorRecibo {

    void emitir(Pago pago, Reserva reserva);
}
