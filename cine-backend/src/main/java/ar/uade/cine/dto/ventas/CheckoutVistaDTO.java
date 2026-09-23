package ar.uade.cine.dto.ventas;

public record CheckoutVistaDTO(String id, int reservaId, String medio, double monto,
                            String urlPago, String codigoQr) {
}
