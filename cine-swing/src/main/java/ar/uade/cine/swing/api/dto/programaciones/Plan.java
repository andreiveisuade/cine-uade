package ar.uade.cine.swing.api.dto.programaciones;

import java.util.List;

// El plan de una grilla, previsualizada o creada: cada pase, si choca, y cuántas generó o salteó.
public record Plan(List<FuncionPlanificada> funciones, int generadas, int salteadas) {
}
