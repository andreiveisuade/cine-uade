package ar.uade.cine.dto.ventas;

import jakarta.validation.constraints.NotBlank;

// Lo que entra al validar el QR en la puerta (POST /api/acceso); Bean Validation exige el código.
public record PedidoAccesoDTO(@NotBlank(message = "Falta el código de acceso") String codigo) {
}
