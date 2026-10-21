package ar.uade.cine.dto.promociones;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al cargar una promoción (POST /api/promociones); exige nombre, tipo y vigencia.
public record PedidoPromocionDTO(
        @Schema(example = "Martes de descuento")
        @NotBlank(message = "Falta el nombre") String nombre,
        @Schema(description = "Define qué campo se usa: PORCENTAJE → porcentaje, MONTO_FIJO → monto, NXM → lleva y paga; los demás se ignoran", allowableValues = {"PORCENTAJE", "MONTO_FIJO", "NXM"}, example = "PORCENTAJE")
        @NotBlank(message = "Falta el tipo de promoción") String tipo,
        @Schema(description = "Solo PORCENTAJE", example = "20.0")
        Double porcentaje, 
        @Schema(description = "Solo MONTO_FIJO")
        Double monto,
        @Schema(description = "Solo NXM: cuántas butacas se llevan")
        Integer lleva, 
        @Schema(description = "Solo NXM: cuántas se pagan")
        Integer paga,
        @Schema(description = "AAAA-MM-DD", example = "2026-11-01")
        @NotBlank(message = "Falta el inicio de la vigencia") String vigenciaDesde,
        @Schema(description = "AAAA-MM-DD", example = "2026-11-30")
        @NotBlank(message = "Falta el fin de la vigencia") String vigenciaHasta,
        @Schema(description = "Nombres en inglés de java.time.DayOfWeek; vacío son todos", allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}, example = "[\"TUESDAY\"]")
        List<String> diasSemana, 
        @Schema(description = "HH:MM, opcional")
        String horaDesde, 
        @Schema(description = "HH:MM, opcional")
        String horaHasta,
        @Schema(description = "Vacío son todos", allowableValues = {"EFECTIVO", "DEBITO", "CREDITO", "QR", "TRANSFERENCIA"}, example = "[]")
        List<String> mediosPago) {
}
