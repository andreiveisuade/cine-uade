package ar.uade.cine.dto.ventas;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

// sesion no es credencial: la doble venta la sigue impidiendo el UNIQUE de la base.
public record PedidoBloqueoDTO(
        @NotBlank(message = "Hace falta una sesión para bloquear butacas") String sesion,
        List<String> butacas) {
}
