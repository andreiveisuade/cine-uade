package ar.uade.cine.service.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.ventas.MedioPago;

/**
 * Cuándo y con qué medio de pago corre una promoción: lo que comparten los tres tipos.
 *
 * <p>Existe para que {@code crearPorcentaje}, {@code crearMontoFijo} y {@code crearNxM} no
 * reciban nueve o diez parámetros sueltos. Seis de ellos viajaban siempre juntos y en el
 * mismo orden, y dos pares eran del mismo tipo —dos {@code LocalDate}, dos
 * {@code LocalTime}—: invertir la vigencia o el horario compilaba igual. Juntos en un record
 * con nombre, lo único que cambia entre los tres métodos es lo que de verdad cambia: el
 * valor del descuento.
 *
 * <p>No valida nada. Las reglas —que la vigencia empiece antes de terminar— siguen en
 * {@link GestorPromociones}, que es donde viven las reglas y donde los tests las esperan.
 * Un conjunto vacío de días o de medios, o una hora en {@code null}, significa "sin
 * restricción", igual que antes.
 */
public record CondicionesPromocion(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias,
                                   LocalTime horaDesde, LocalTime horaHasta,
                                   Set<MedioPago> mediosPago) {
}
