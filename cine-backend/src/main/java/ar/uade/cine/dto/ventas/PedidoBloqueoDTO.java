package ar.uade.cine.dto.ventas;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al bloquear butacas (POST /api/funciones/{id}/bloqueos); va la selección entera, exige sesion.
public record PedidoBloqueoDTO(
        @Schema(description = "Identifica la compra en curso; no es una credencial", example = "sesion-demo-1")
        @NotBlank(message = "Falta la sesión para bloquear butacas") String sesion,
        @Schema(description = "Códigos de butaca de la selección entera", example = "[\"A1\", \"A2\"]")
        List<String> butacas) {
}
