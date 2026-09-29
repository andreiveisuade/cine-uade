package ar.uade.cine.service.ventas;

// Aviso de reserva vendida; evento del Observer que publica GestorReservas y oye ComprobantesDeVentas.
// Solo el id: el que escucha corre después del commit, con otra sesión, y relee la reserva completa.
public record ReservaCreada(int reservaId) {
}
