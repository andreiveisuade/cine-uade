package ar.uade.cine.swing.api.dto.candy;

import java.util.Map;

// Sin `reservaId` es venta de mostrador; `cantidades` es productoId → cantidad.
public record PedidoVenta(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades, String medio,
                          String codigoAutorizacion) {
}
