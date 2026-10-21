package ar.uade.cine.dto.salas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al marcar o reponer una butaca (PATCH /api/salas/{salaId}/asientos/{codigo}); exige el estado.
public record PedidoEstadoDTO(
        @Schema(allowableValues = {"HABILITADO", "FUERA_DE_SERVICIO"}, example = "FUERA_DE_SERVICIO")
        @NotBlank(message = "Falta el estado de la butaca") String estado) {
}
