package ar.uade.cine.swing.api.dto;

public record Pago(int id, int reservaId, double subtotal, Integer promocionId, double descuento, double monto,
                   String medio, String fecha, String codigoAutorizacion, Pelicula pelicula, Cliente cliente,
                   Integer entradas) {
}
