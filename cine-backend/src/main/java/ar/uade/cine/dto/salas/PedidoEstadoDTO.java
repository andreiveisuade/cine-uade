package ar.uade.cine.dto.salas;

import jakarta.validation.constraints.NotBlank;

public record PedidoEstadoDTO(@NotBlank(message = "Falta el estado de la butaca") String estado) {
}
