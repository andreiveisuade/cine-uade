package ar.uade.cine.service.programaciones;

import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.model.programaciones.Programacion;

public record PlanProgramacion(Programacion programacion, List<FuncionPlanificada> funciones) {

    public record FuncionPlanificada(LocalDateTime inicio, boolean choca, String motivo) {
    }

    public List<FuncionPlanificada> programables() {
        return funciones.stream().filter(f -> !f.choca()).toList();
    }

    public List<FuncionPlanificada> salteadas() {
        return funciones.stream().filter(FuncionPlanificada::choca).toList();
    }
}
