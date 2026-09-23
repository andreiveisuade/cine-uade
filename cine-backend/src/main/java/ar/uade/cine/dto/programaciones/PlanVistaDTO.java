package ar.uade.cine.dto.programaciones;

import java.util.List;

public record PlanVistaDTO(ProgramacionVistaDTO programacion, List<FuncionPlanificadaVistaDTO> funciones,
                        int generadas, int salteadas) {
}
