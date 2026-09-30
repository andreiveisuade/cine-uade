package ar.uade.cine.dto.ventas;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

// Lo que entra al bloquear butacas (POST /api/funciones/{id}/bloqueos); va la selección entera, exige sesion.
// sesion no es credencial: la doble venta la sigue impidiendo el UNIQUE de la base.
public record PedidoBloqueoDTO(
        @NotBlank(message = "Falta la sesión para bloquear butacas") String sesion,
        List<String> butacas) {
}
