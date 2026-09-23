package ar.uade.cine.service.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;

/**
 * Lo que hace falta para previsualizar o dar de alta una grilla recurrente.
 *
 * <p>Es el mismo criterio que {@link CriteriosGrilla}: {@code previsualizar} y {@code crear}
 * recibían nueve parámetros sueltos, con dos {@code int} seguidos (película y sala) y dos
 * {@code LocalDate} seguidos (desde y hasta) que se podían invertir sin que el compilador
 * avise. Además los dos métodos tienen que recibir <em>exactamente</em> lo mismo —el
 * informe de la previsualización tiene que predecir el alta—, y un solo record lo
 * garantiza por construcción.
 *
 * <p>No valida: las reglas de la grilla siguen en {@link GestorProgramaciones} y las de
 * cada función en {@code GestorFunciones}. {@code hasta} en {@code null} es una grilla
 * abierta.
 */
public record DatosGrilla(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                          LocalTime horaInicio, Set<DayOfWeek> diasSemana, Version version,
                          Proyeccion proyeccion, Dinero precio) {
}
