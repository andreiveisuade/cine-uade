package ar.uade.cine.service.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;

/**
 * Datos de una grilla recurrente. Un solo record para previsualizar y crear garantiza que
 * reciban lo mismo, y evita invertir los {@code int} o {@code LocalDate} seguidos. No
 * valida. {@code hasta} en {@code null} es una grilla abierta.
 */
public record DatosGrilla(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                          LocalTime horaInicio, Set<DayOfWeek> diasSemana, Version version,
                          Proyeccion proyeccion, Dinero precio) {
}
