package ar.uade.cine.swing.api.dto;

import java.util.List;

public record Plan(Programacion programacion, List<FuncionPlanificada> funciones, int generadas, int salteadas) {
}
