package ar.uade.cine.servicio;

import ar.uade.cine.interfaces.Asiento;
import ar.uade.cine.interfaces.Funcion;
import ar.uade.cine.interfaces.Sala;

/**
 * El precio de una butaca sale del precio base de la función, ajustado por el formato
 * de la sala y por el tipo de butaca: una VIP en IMAX no vale lo mismo que una estándar
 * en 2D.
 *
 * <p>Es una clase concreta y no una interfaz porque hoy hay una sola forma de calcular.
 * Cuando aparezcan promociones (miércoles 2x1, jubilados) va a haber varias implementaciones
 * de verdad y ahí se extrae el contrato.
 */
public class CalculadoraPrecio {

    public double precioDe(Funcion funcion, Sala sala, Asiento asiento) {
        return funcion.getPrecio()
                * sala.getTipo().getMultiplicadorPrecio()
                * asiento.getTipo().getMultiplicadorPrecio();
    }
}
