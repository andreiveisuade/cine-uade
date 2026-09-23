package ar.uade.cine.dto.programaciones;

import java.util.List;

/**
 * {@code generadas} y {@code salteadas} se cuentan en el servidor para que cada pantalla no
 * las cuente a su manera.
 */
public record PlanVistaDTO(ProgramacionVistaDTO programacion, List<FuncionPlanificadaVistaDTO> funciones,
                        int generadas, int salteadas) {
}
