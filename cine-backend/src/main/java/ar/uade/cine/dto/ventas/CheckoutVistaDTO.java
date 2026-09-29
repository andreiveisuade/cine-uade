package ar.uade.cine.dto.ventas;

// El checkout electrónico abierto (POST /api/reservas/{id}/checkout); monto con descuento, codigoQr es texto.
public record CheckoutVistaDTO(String id, int reservaId, String medio, double monto,
                            String urlPago, String codigoQr) {
}
