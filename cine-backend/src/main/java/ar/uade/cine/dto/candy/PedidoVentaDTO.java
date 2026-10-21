package ar.uade.cine.dto.candy;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

// Lo que entra al vender candy (POST /api/candy/compras); exige ítems y medio, sin reservaId es mostrador.
public record PedidoVentaDTO(
        @Schema(description = "Opcional: el cliente que compra")
        Integer clienteId, 
        @Schema(description = "Opcional: sin reserva es venta de mostrador")
        Integer reservaId,
        @Schema(description = "id de producto → cantidad (1 a 20 por producto)", example = "{\"1\": 2}")
        @NotEmpty(message = "Hay que elegir al menos un producto") Map<Integer, Integer> cantidades,
        @Schema(allowableValues = {"EFECTIVO", "DEBITO", "CREDITO", "QR", "TRANSFERENCIA"}, example = "EFECTIVO")
        @NotBlank(message = "Falta el medio de pago") String medio,
        @Schema(description = "Solo si el medio lo requiere (GET /api/medios-pago)")
        String codigoAutorizacion) {
}
