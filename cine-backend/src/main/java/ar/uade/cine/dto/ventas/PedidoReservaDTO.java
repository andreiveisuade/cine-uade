package ar.uade.cine.dto.ventas;

import java.util.List;
import java.util.Map;

import ar.uade.cine.model.ventas.TipoTarifa;

/**
 * {@code butacas} es código de butaca a tarifa. {@code codigos} es la forma sin tarifas, que
 * se acepta como todas GENERAL. {@code sesion} es opcional: evita que el propio bloqueo
 * rechace la reserva, y la boletería no la tiene.
 */
public record PedidoReservaDTO(Integer funcionId, String nombre, String email,
                            List<String> codigos, Map<String, TipoTarifa> butacas,
                            String sesion) {
}
