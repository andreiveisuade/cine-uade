package ar.uade.cine.swing.api.dto.candy;

import java.util.Map;

// Lo que Swing manda al vender candy, en mostrador o sumado a una reserva; el total lo calcula el backend.
// Sin `reservaId` es venta de mostrador; `cantidades` es productoId → cantidad.
public record PedidoVenta(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades, String medio,
                          String codigoAutorizacion) {
}
