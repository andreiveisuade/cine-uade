package ar.uade.cine.service.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.ventas.MedioPago;

/**
 * Cuándo y con qué medio corre una promoción, común a los tres tipos. Record para no
 * invertir sin aviso dos {@code LocalDate} o dos {@code LocalTime}. No valida: eso es de
 * {@link GestorPromociones}. Vacío o {@code null} significa sin restricción.
 */
public record CondicionesPromocion(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias,
                                   LocalTime horaDesde, LocalTime horaHasta,
                                   Set<MedioPago> mediosPago) {
}
