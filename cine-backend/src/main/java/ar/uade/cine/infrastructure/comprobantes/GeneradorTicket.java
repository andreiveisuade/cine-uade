package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.ventas.Reserva;

// Puerto: el ticket de una reserva; ComprobantesDeVentas no conoce el formato (Variaciones protegidas).
public interface GeneradorTicket {

    // Todo sale de la reserva: función, película, sala y cliente son relaciones LAZY que se
    // navegan acá, así que se llama adentro de la transacción de solo lectura que ComprobantesDeVentas
    // abre después del commit de la venta, con la sesión abierta. Afuera tiraría LazyInitializationException.
    void emitir(Reserva reserva);
}
