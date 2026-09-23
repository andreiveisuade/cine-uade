package ar.uade.cine.swing.api.dto;

public record Pago(int reservaId, double subtotal, double descuento, double monto, String medio, String fecha,
                   String codigoAutorizacion, Pelicula pelicula, Cliente cliente) {
}
