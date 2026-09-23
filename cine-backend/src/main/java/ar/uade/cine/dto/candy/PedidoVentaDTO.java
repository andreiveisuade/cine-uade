package ar.uade.cine.dto.candy;

import java.util.Map;

public record PedidoVentaDTO(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades,
                          String medio, String codigoAutorizacion) {
}
