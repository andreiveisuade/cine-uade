package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.ventas.MedioPago;

// Cuándo corre una promoción (vigencia, días, horario, medios), tal como llega; Parameter Object.
// Datos crudos, con null en lo que no vino: los valida Promocion, y armarlos antes cambiaría qué error sale primero.
public record CondicionesPromocion(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias,
                                   LocalTime horaDesde, LocalTime horaHasta,
                                   Set<MedioPago> mediosPago) {
}
