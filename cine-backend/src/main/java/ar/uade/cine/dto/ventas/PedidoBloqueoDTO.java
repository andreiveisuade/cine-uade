package ar.uade.cine.dto.ventas;

import java.util.List;

/**
 * Lo que esa sesión tiene elegido en el mapa, entero y no de a una butaca: el pedido dice
 * cuál es la selección ahora, y el gestor toma lo nuevo, renueva lo que sigue y suelta lo
 * que se deseleccionó. Con {@code butacas} vacío es "ya no estoy eligiendo nada".
 *
 * <p>{@code sesion} lo genera el navegador y no lleva ninguna credencial. No hace falta:
 * lo peor que puede hacer una sesión inventada es soltar el bloqueo de otro, y eso devuelve
 * una butaca a la venta sin poder venderla dos veces —eso lo sigue arbitrando el
 * {@code UNIQUE} de la base—.
 */
public record PedidoBloqueoDTO(String sesion, List<String> butacas) {
}
