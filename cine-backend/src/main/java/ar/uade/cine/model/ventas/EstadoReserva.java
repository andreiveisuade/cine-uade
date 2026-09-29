package ar.uade.cine.model.ventas;

// Ciclo de vida de una reserva; solo RESERVADA cambia: se cobra, se cancela o expira (R5, R13, R17).
public enum EstadoReserva {

    RESERVADA,
    PAGADA,
    CANCELADA,

    EXPIRADA
}
