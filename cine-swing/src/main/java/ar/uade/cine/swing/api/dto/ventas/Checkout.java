package ar.uade.cine.swing.api.dto.ventas;

// Un checkout abierto en la pasarela emulada; el monto ya trae el descuento del medio elegido.
// `codigoQr` es texto: la pasarela es emulada y dibujarlo como imagen la haría parecer real.
public record Checkout(String id, int reservaId, String medio, double monto, String urlPago, String codigoQr) {
}
