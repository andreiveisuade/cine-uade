package ar.uade.cine.service.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;

public record DatosGrilla(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                          LocalTime horaInicio, Set<DayOfWeek> diasSemana, Version version,
                          Proyeccion proyeccion, Dinero precio) {
}
