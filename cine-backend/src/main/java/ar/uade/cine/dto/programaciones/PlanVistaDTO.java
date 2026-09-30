package ar.uade.cine.dto.programaciones;

import java.util.List;

// El plan de una programación (POST /api/programaciones y /previsualizacion): cada pase, si choca, y totales.
public record PlanVistaDTO(ProgramacionVistaDTO programacion, List<FuncionPlanificadaVistaDTO> funciones,
                           int generadas, int salteadas) {
}
