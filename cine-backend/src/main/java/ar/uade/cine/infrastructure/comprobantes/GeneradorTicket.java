package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.ventas.Reserva;

// Puerto: el ticket de una reserva; ComprobantesDeVentas no conoce el formato (Variaciones protegidas).
public interface GeneradorTicket {

    // Navega relaciones LAZY: llamar dentro de la transacción de solo lectura de ComprobantesDeVentas,
    // o tira LazyInitializationException.
    void emitir(Reserva reserva);
}
