package ar.uade.cine.service.informes;

import ar.uade.cine.model.dinero.Dinero;

/**
 * Cuánto dejó una función entre entradas y candy. La boletería es el {@link Bordero}, no
 * se recalcula, para no tener dos definiciones de lo recaudado.
 *
 * @param comprasCandy va junto al monto porque dice si el upsell de la web sirve
 */
public record InformeFuncion(Bordero bordero, int comprasCandy, Dinero candy, Dinero total) {
}
