package ar.uade.cine.dto.salas;

/**
 * Lo que se puede cambiar de una sala ya creada. Las butacas no están: se fijan al crearla.
 * Sin minutosLimpieza, se conserva el que tenía.
 */
public record PedidoEdicionSalaDTO(String nombre, String tipo, Integer minutosLimpieza) {
}
