package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.ventas.Reserva;

// Puerto: el ticket de una reserva; GestorReservas no conoce el formato (Variaciones protegidas).
public interface GeneradorTicket {

    // Todo sale de la reserva: función, película, sala y cliente son relaciones LAZY que se
    // navegan acá, así que se llama adentro del @Transactional de GestorReservas, con la sesión
    // abierta. Afuera de él tiraría LazyInitializationException.
    void emitir(Reserva reserva);
}
