package ar.uade.cine.dto.comun;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Lo que entra al dar de baja o reactivar una promoción o una programación (PATCH); exige activa.
public record PedidoActivacionDTO(
        @Schema(description = "false la da de baja, true la reactiva", example = "false")
        @NotNull(message = "Falta decir si queda activa") Boolean activa) {
}
