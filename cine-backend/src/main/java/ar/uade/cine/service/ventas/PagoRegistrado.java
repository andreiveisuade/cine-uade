package ar.uade.cine.service.ventas;

// Aviso de que se cobró una reserva; evento del Observer que publica GestorPagos y oye ComprobantesDeVentas.
// Se publica con cualquier medio: si lleva recibo lo decide el pago, no quien avisa.
public record PagoRegistrado(int pagoId) {
}
