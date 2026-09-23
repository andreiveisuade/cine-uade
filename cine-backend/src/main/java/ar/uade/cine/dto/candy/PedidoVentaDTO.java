package ar.uade.cine.dto.candy;

import java.util.Map;

/**
 * {@code cantidades} es id de producto a unidades. Sin cliente ni reserva es venta de
 * mostrador; con reserva, el cliente sale de ella.
 */
public record PedidoVentaDTO(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades,
                          String medio, String codigoAutorizacion) {
}
