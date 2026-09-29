package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.ventas.MedioPago;

// Cuándo corre una promoción (vigencia, días, horario, medios), tal como llega; Parameter Object.
// Vive en model/ para que los constructores la reciban entera en vez de seis parámetros sueltos. Trae
// los datos crudos, con null en lo que no vino: los valida Promocion, que arma con ellos la vigencia
// (Periodo) y la franja (FranjaHoraria). Armarlos antes, en el controller, cambiaría qué error sale
// primero. Días y medios siguen en sus dos tablas de colección.
public record CondicionesPromocion(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias,
                                   LocalTime horaDesde, LocalTime horaHasta,
                                   Set<MedioPago> mediosPago) {
}
