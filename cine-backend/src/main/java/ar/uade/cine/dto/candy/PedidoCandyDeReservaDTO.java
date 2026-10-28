package ar.uade.cine.dto.candy;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Lo que entra al pedir candy para la compra (PUT /api/reservas/codigo/{codigo}/candy); vacío saca el pedido.
public record PedidoCandyDeReservaDTO(
        @Schema(description = "id de producto → cantidad (1 a 20 por producto); {} saca el pedido",
                example = "{\"4\": 1}")
        @NotNull(message = "Faltan las cantidades") Map<Integer, Integer> cantidades) {
}
