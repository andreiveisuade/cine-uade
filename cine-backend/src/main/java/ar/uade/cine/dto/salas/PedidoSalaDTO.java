package ar.uade.cine.dto.salas;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

// Lo que entra al dar de alta una sala (POST /api/salas); exige nombre, tipo y filas, no la limpieza.
public record PedidoSalaDTO(
        @Schema(description = "Único entre las salas", example = "Sala 5")
        @NotBlank(message = "Falta el nombre") String nombre,
        @Schema(allowableValues = {"DOS_D", "TRES_D", "IMAX", "CUATRO_D"}, example = "DOS_D")
        @NotBlank(message = "Falta el tipo de sala") String tipo,
        @Schema(description = "Una cantidad por fila; las filas se rotulan A, B, C…", example = "[10, 10, 12]")
        @NotEmpty(message = "La sala tiene que tener al menos una fila") List<Integer> butacasPorFila,
        @Schema(description = "Códigos de butaca, p. ej. B5", example = "[\"B5\"]")
        List<String> codigosVip, 
        @Schema(description = "Códigos de butaca", example = "[]")
        List<String> codigosPareja,
        @Schema(description = "Códigos de butaca", example = "[\"A1\"]")
        List<String> codigosAccesibles, 
        @Schema(description = "De 0 a 120; sin el campo, el default", example = "15")
        Integer minutosLimpieza) {
}
