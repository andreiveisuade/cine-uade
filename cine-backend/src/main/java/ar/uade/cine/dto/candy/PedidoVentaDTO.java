package ar.uade.cine.dto.candy;

import java.util.Map;

/**
 * {@code cantidades} es id de producto a unidades. {@code clienteId} y {@code reservaId}
 * son los dos opcionales: sin ninguno es una venta de mostrador, y con reserva el
 * cliente sale de ella.
 */
public record PedidoVentaDTO(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades,
                          String medio, String codigoAutorizacion) {
}
