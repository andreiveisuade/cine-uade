package ar.uade.cine.dto.salas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al editar una sala (PUT /api/salas/{id}); exige nombre y tipo, sin limpieza queda la anterior.
public record PedidoEdicionSalaDTO(
        @Schema(example = "Sala 5")
        @NotBlank(message = "Falta el nombre") String nombre,
        @Schema(allowableValues = {"DOS_D", "TRES_D", "IMAX", "CUATRO_D"}, example = "DOS_D")
        @NotBlank(message = "Falta el tipo de sala") String tipo,
        @Schema(description = "Sin el campo queda la anterior", example = "15")
        Integer minutosLimpieza) {
}
