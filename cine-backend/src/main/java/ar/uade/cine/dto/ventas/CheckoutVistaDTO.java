package ar.uade.cine.dto.ventas;

/**
 * El checkout abierto contra la pasarela: lo que el front necesita para mandar a pagar.
 *
 * <p>{@code monto} ya lleva el descuento resuelto, porque es el importe que el cliente va a
 * aprobar en la pantalla del procesador. {@code codigoQr} es el <em>contenido</em> del QR y
 * no una imagen: dibujarlo es del lado del navegador, igual que traducir los enums.
 */
public record CheckoutVistaDTO(String id, int reservaId, String medio, double monto,
                            String urlPago, String codigoQr) {
}
