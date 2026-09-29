package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import ar.uade.cine.model.ventas.MedioPago;

// Cuándo corre una promoción (vigencia, días, horario, medios), común a las tres clases; Value Object.
// Vive en model/ para que los constructores la reciban entera en vez de seis parámetros sueltos.
// Es un valor plano y no un @Embeddable:
// Promocion sigue mapeando cada campo a su columna y sus dos tablas de colección, así que el
// schema no se mueve. Como embeddable había que renombrar desde/hasta o pisarlos con
// @AttributeOverride para no perder vigencia_desde y vigencia_hasta, y llevar las dos
// @ElementCollection adentro del valor, sin ganar nada que alguna consulta use.
public record CondicionesPromocion(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias,
                                   LocalTime horaDesde, LocalTime horaHasta,
                                   Set<MedioPago> mediosPago) {
}
