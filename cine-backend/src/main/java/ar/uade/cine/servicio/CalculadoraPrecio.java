package ar.uade.cine.servicio;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.Sala;

/**
 * El precio de una butaca sale del precio base de la función, ajustado por la tecnología
 * de la sala y por el tipo de butaca: una VIP en IMAX no vale lo mismo que una estándar
 * en 2D.
 *
 * <p>Es una clase concreta y no una interfaz porque hoy hay una sola forma de calcular.
 * Cuando aparezcan promociones (miércoles 2x1, jubilados) va a haber varias implementaciones
 * de verdad y ahí se extrae el contrato.
 */
public class CalculadoraPrecio {

    public double precioDe(Funcion funcion, Sala sala, Asiento asiento) {
        double bruto = funcion.getPrecio()
                * sala.getTipo().getMultiplicadorPrecio()
                * asiento.getTipo().getMultiplicadorPrecio();
        return redondear(bruto);
    }

    /**
     * Lo que sale la butaca más barata de esa función: la estándar, que no tiene recargo
     * por tipo. Es el "desde $" de la cartelera.
     */
    public double precioBaseEnSala(Funcion funcion, Sala sala) {
        return redondear(funcion.getPrecio() * sala.getTipo().getMultiplicadorPrecio());
    }

    /**
     * A dos decimales. Multiplicar doubles arrastra error en cuanto el precio tiene
     * centavos: 5250.50 x 1.3 da 6825.650000000001, y ese resto terminaría impreso en
     * el ticket y sumado al total de la reserva.
     */
    static double redondear(double monto) {
        return Math.round(monto * 100) / 100.0;
    }
}
