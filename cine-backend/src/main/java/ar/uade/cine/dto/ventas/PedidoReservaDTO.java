package ar.uade.cine.dto.ventas;

import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.ventas.TipoTarifa;

/**
 * {@code butacas} es el pedido completo: código de butaca a tarifa de quien la ocupa.
 * {@code codigos} es la forma vieja, sin tarifas, y se sigue aceptando para no romper
 * a quien ya la use: se interpreta como todas GENERAL.
 */
public record PedidoReservaDTO(Integer funcionId, String nombre, String email,
                            List<String> codigos, Map<String, TipoTarifa> butacas) {
}
