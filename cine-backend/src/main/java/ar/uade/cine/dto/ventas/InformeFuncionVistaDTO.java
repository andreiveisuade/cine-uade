package ar.uade.cine.dto.ventas;

/**
 * Cuánto recaudó una función entre sus dos cajas. {@code boleteria} es el mismo borderó que
 * devuelve su propio endpoint —no una versión recortada— justamente para que los dos
 * informes no puedan decir números distintos de lo mismo.
 *
 * <p>{@code candy} es solo el que se pudo atribuir a esta función: el del mostrador no
 * dice a qué función va quien lo compró y se cuenta en el arqueo del día.
 */
public record InformeFuncionVistaDTO(BorderoVistaDTO boleteria, int comprasCandy, double candy,
                                  double total) {
}
