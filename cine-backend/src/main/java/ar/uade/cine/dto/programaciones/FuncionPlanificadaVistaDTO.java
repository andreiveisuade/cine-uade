package ar.uade.cine.dto.programaciones;

import com.fasterxml.jackson.annotation.JsonInclude;

// Un pase del plan de una programación, dentro de PlanVistaDTO; omite nulos: motivo solo viaja si choca.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FuncionPlanificadaVistaDTO(String inicio, boolean choca, String motivo) {
}
