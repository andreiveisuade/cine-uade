package ar.uade.cine.dto.salas;

import jakarta.validation.constraints.NotBlank;

// Lo que entra al marcar o reponer una butaca (PATCH /api/salas/{salaId}/asientos/{codigo}); exige el estado.
public record PedidoEstadoDTO(@NotBlank(message = "Falta el estado de la butaca") String estado) {
}
