package ar.uade.cine.dto.ventas;

/**
 * {@code monto} ya lleva el descuento: es lo que el cliente aprueba en la pasarela.
 * {@code codigoQr} es el contenido del QR, no una imagen: lo dibuja el navegador.
 */
public record CheckoutVistaDTO(String id, int reservaId, String medio, double monto,
                            String urlPago, String codigoQr) {
}
