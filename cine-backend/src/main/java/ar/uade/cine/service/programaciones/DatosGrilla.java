package ar.uade.cine.service.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.funciones.FormatoDeFuncion;

// Pedido de alta de una programación; record de entrada sin reglas: las aplican Programacion y el gestor.
public record DatosGrilla(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                          LocalTime horaInicio, Set<DayOfWeek> diasSemana,
                          FormatoDeFuncion formato) {
}
