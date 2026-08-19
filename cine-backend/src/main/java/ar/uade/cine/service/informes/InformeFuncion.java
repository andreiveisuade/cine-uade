package ar.uade.cine.service.informes;

import ar.uade.cine.model.dinero.Dinero;

/**
 * Cuánto dejó una función, sumando las dos cosas que se venden alrededor de ella: las
 * entradas y el candy.
 *
 * <p>La parte de boletería no se vuelve a calcular: es el {@link Bordero}, que ya la
 * resolvió para el INCAA. Repetir esa cuenta acá habría dejado dos definiciones de
 * «cuánto recaudó la función» que se separan a la primera corrección, que es el mismo
 * motivo por el que el arqueo vive en el gestor y no en cada pantalla.
 *
 * @param comprasCandy cuántas compras del candy se le pudieron atribuir. Va junto al monto
 *                     porque es lo que dice si el <em>upsell</em> de la web sirve: mucha
 *                     plata en dos compras no es lo mismo que la misma plata en veinte
 * @param candy lo que entró por la barra atribuido a esta función
 * @param total la suma de las dos cajas, que es el número por el que se pregunta
 */
public record InformeFuncion(Bordero bordero, int comprasCandy, Dinero candy, Dinero total) {
}
