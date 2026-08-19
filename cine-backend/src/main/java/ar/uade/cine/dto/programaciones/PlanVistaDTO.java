package ar.uade.cine.dto.programaciones;

import java.util.List;

/**
 * El informe de una grilla. {@code generadas} y {@code salteadas} son cuentas que el front
 * podría hacer solas, pero son justo lo que se muestra arriba de todo —"12 funciones, 3 se
 * saltearon"— y contarlas del lado del servidor evita que cada pantalla las cuente a su
 * manera.
 */
public record PlanVistaDTO(ProgramacionVistaDTO programacion, List<FuncionPlanificadaVistaDTO> funciones,
                        int generadas, int salteadas) {
}
