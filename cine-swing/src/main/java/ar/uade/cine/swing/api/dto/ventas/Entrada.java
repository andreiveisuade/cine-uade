package ar.uade.cine.swing.api.dto.ventas;

// Una entrada de una reserva: la butaca, su tarifa por constante y el precio ya calculado.
public record Entrada(String codigo, String tarifa, double precio) {
}
