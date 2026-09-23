package ar.uade.cine.dto.candy;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record PedidoVentaDTO(Integer clienteId, Integer reservaId,
                             @NotEmpty(message = "Hay que elegir al menos un producto") Map<Integer, Integer> cantidades,
                             @NotBlank(message = "Falta el medio de pago") String medio,
                             String codigoAutorizacion) {
}
