package ar.uade.cine.swing.api.dto;

// `ocupado` es de la función (R4) y solo viene en el mapa de una función; `estado`, del asiento (R9).
public record Asiento(int id, int salaId, int fila, int numero, String codigo, String tipo, String estado,
                      Boolean ocupado, Double precio) {
}
