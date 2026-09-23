package ar.uade.cine.dto.programaciones;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FuncionPlanificadaVistaDTO(String inicio, boolean choca, String motivo) {
}
