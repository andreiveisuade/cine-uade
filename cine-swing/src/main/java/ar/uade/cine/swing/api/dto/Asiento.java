package ar.uade.cine.swing.api.dto;

// Sin `ocupado` ni `precio`: son del mapa de una función, que esta app no muestra. `estado` es del asiento (R9).
public record Asiento(int id, int fila, int numero, String codigo, String tipo, String estado) {
}
