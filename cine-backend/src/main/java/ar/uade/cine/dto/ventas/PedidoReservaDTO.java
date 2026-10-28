package ar.uade.cine.dto.ventas;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Lo que entra al reservar (POST /api/reservas), sin cliente: es el de la sesión. Butacas va código → tarifa.
// La tarifa viaja como texto: la traduce el controller con Parseo, así acepta minúsculas y el error nombra la butaca.
public record PedidoReservaDTO(
        @Schema(example = "1")
        @NotNull(message = "Falta la función") Integer funcionId,
        @Schema(description = "Código de butaca → tarifa (GENERAL, MENOR, JUBILADO, ESTUDIANTE); sin tarifa es GENERAL", example = "{\"A1\": \"GENERAL\", \"A2\": \"GENERAL\"}")
        Map<String, String> butacas,
        @Schema(description = "La misma del bloqueo previo", example = "sesion-demo-1")
        String sesion) {
}
