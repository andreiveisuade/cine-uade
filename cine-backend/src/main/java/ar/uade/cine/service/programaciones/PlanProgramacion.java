package ar.uade.cine.service.programaciones;

import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.service.informes.Arqueo;

/**
 * Qué funciones genera una grilla, fecha por fecha, y cuáles no puede generar.
 *
 * <p>Es lo que devuelven tanto la previsualización como el alta, y a propósito es el
 * mismo tipo: las dos hacen exactamente la misma cuenta y solo se diferencian en si
 * escriben. Que el informe fuera distinto según el caso invitaría a que la cuenta también
 * se separara.
 *
 * <p>Es el resultado de una cuenta del negocio, no una forma de mostrarla —mismo criterio
 * que {@link Arqueo}—: los momentos van como {@code LocalDateTime} y no formateados.
 *
 * @param programacion la grilla evaluada. En una previsualización todavía no tiene id:
 *                     existe en memoria y no se guardó
 */
public record PlanProgramacion(Programacion programacion, List<FuncionPlanificada> funciones) {

    /**
     * Una fecha del rango. {@code motivo} explica el choque y es null cuando no lo hay:
     * el administrador tiene que poder leer contra qué se pisa cada función que se saltea,
     * no solo cuántas fueron.
     */
    public record FuncionPlanificada(LocalDateTime inicio, boolean choca, String motivo) {
    }

    /** Las que la sala tiene libres: las que el alta guarda de verdad. */
    public List<FuncionPlanificada> programables() {
        return funciones.stream().filter(f -> !f.choca()).toList();
    }

    /** Las que chocan contra algo ya programado y el alta saltea. */
    public List<FuncionPlanificada> salteadas() {
        return funciones.stream().filter(FuncionPlanificada::choca).toList();
    }
}
