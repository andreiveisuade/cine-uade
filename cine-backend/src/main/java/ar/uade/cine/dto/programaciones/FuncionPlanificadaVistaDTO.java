package ar.uade.cine.dto.programaciones;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Una fecha del rango. {@code motivo} explica contra qué choca y viaja en null cuando
 * no hay choque: el administrador tiene que poder leer por qué se saltea cada una.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FuncionPlanificadaVistaDTO(String inicio, boolean choca, String motivo) {
}
