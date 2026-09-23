package ar.uade.cine.service.programaciones;

import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.model.programaciones.Programacion;

/**
 * Qué funciones genera una grilla y cuáles saltea. Mismo tipo para previsualizar y crear
 * porque hacen la misma cuenta.
 *
 * @param programacion en una previsualización todavía no tiene id
 */
public record PlanProgramacion(Programacion programacion, List<FuncionPlanificada> funciones) {

    /** {@code motivo} dice contra qué choca, o null si no choca. */
    public record FuncionPlanificada(LocalDateTime inicio, boolean choca, String motivo) {
    }

    public List<FuncionPlanificada> programables() {
        return funciones.stream().filter(f -> !f.choca()).toList();
    }

    public List<FuncionPlanificada> salteadas() {
        return funciones.stream().filter(FuncionPlanificada::choca).toList();
    }
}
