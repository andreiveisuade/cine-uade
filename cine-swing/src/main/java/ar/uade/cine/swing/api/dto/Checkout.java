package ar.uade.cine.swing.api.dto;

// `codigoQr` es texto: la pasarela es emulada y dibujarlo como imagen la haría parecer real.
public record Checkout(String id, int reservaId, String medio, double monto, String urlPago, String codigoQr) {
}
