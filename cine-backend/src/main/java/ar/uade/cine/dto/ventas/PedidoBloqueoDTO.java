package ar.uade.cine.dto.ventas;

import java.util.List;

/**
 * La selección entera de esa sesión, no de a una butaca; {@code butacas} vacío la suelta.
 * {@code sesion} no es credencial: una inventada solo puede soltar un bloqueo ajeno, y la
 * doble venta la sigue impidiendo el {@code UNIQUE} de la base.
 */
public record PedidoBloqueoDTO(String sesion, List<String> butacas) {
}
