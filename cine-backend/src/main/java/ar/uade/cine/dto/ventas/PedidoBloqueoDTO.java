package ar.uade.cine.dto.ventas;

import java.util.List;

// sesion no es credencial: la doble venta la sigue impidiendo el UNIQUE de la base.
public record PedidoBloqueoDTO(String sesion, List<String> butacas) {
}
