package ar.uade.cine.service.programaciones;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.programaciones.Programacion;

// Lo que genera una programación, horario por horario, y lo que saltea por R3; record de resultado.
public record PlanProgramacion(Programacion programacion, List<FuncionPlanificada> funciones) {

    // Experto: el horario sabe describirse, programable o salteado por la función con la que choca.
    public record FuncionPlanificada(LocalDateTime inicio, boolean choca, String motivo) {

        private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        static FuncionPlanificada programable(LocalDateTime inicio) {
            return new FuncionPlanificada(inicio, false, null);
        }

        static FuncionPlanificada salteadaPor(LocalDateTime inicio, Funcion choque) {
            return new FuncionPlanificada(inicio, true, "la sala ya tiene la función " + choque.getId()
                    + " a las " + choque.getInicio().format(MOMENTO));
        }
    }

    public List<FuncionPlanificada> programables() {
        return funciones.stream().filter(f -> !f.choca()).toList();
    }

    public List<FuncionPlanificada> salteadas() {
        return funciones.stream().filter(FuncionPlanificada::choca).toList();
    }
}
