package ar.uade.cine.dto.programaciones;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code motivo} dice contra qué choca, en null si no choca. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FuncionPlanificadaVistaDTO(String inicio, boolean choca, String motivo) {
}
