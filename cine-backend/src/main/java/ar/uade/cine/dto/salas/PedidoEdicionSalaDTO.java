package ar.uade.cine.dto.salas;

/**
 * Las butacas no se editan: se fijan al crear la sala. Sin {@code minutosLimpieza} se
 * conserva el que tenía.
 */
public record PedidoEdicionSalaDTO(String nombre, String tipo, Integer minutosLimpieza) {
}
