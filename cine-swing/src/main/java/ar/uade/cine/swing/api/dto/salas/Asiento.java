package ar.uade.cine.swing.api.dto.salas;

// Una butaca de una sala como la lee el mapa de Salas, con su tipo y su estado físico.
// Sin `ocupado` ni `precio`: son del mapa de una función, que acá no se ve; `estado` es del asiento (R9).
public record Asiento(int id, int fila, int numero, String codigo, String tipo, String estado) {
}
