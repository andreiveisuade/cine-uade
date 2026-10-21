package ar.uade.cine.service.ventas;

// Cómo quedó la reserva cuando el comprador vuelve de la pasarela; el aviso es null si quedó cobrada.
public record RetornoCheckout(String codigoReserva, String aviso) {
}
