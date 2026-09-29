package ar.uade.cine.swing.api.dto.programaciones;

import java.util.List;

public record Plan(List<FuncionPlanificada> funciones, int generadas, int salteadas) {
}
