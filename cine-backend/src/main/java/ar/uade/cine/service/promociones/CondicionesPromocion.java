package ar.uade.cine.service.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.ventas.MedioPago;

public record CondicionesPromocion(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias,
                                   LocalTime horaDesde, LocalTime horaHasta,
                                   Set<MedioPago> mediosPago) {
}
