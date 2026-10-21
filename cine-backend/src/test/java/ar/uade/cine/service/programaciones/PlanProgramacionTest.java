package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ar.uade.cine.service.programaciones.PlanProgramacion.FuncionPlanificada;

class PlanProgramacionTest {

    @Test
    void unaFuncionProgramableNoChocaNiTieneMotivo() {
        FuncionPlanificada funcion = FuncionPlanificada.programable(LocalDateTime.of(2026, 9, 9, 21, 0));

        assertFalse(funcion.choca());
        assertNull(funcion.motivo());
    }
}
