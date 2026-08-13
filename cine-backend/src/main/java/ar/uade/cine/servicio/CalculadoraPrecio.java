package ar.uade.cine.servicio;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.ventas.TipoTarifa;

/**
 * El precio <strong>de lista</strong> de una butaca: el precio base de la función ajustado
 * por la tecnología de la sala, por el tipo de butaca y por quién la compra. Una VIP en
 * IMAX no vale lo mismo que una estándar en 2D, y un jubilado no paga lo mismo que un
 * adulto en la misma butaca.
 *
 * <p>Acá termina el cálculo por butaca. Los descuentos por promoción <em>no</em> pasan por
 * esta clase: se calculan sobre el total de la reserva al cobrar, porque un 2x1 no se
 * puede expresar como un factor sobre una butaca sola.
 */
public class CalculadoraPrecio {

    public double precioDe(Funcion funcion, Sala sala, Asiento asiento, TipoTarifa tarifa) {
        double bruto = funcion.getPrecio()
                * sala.getTipo().getMultiplicadorPrecio()
                * asiento.getTipo().getMultiplicadorPrecio()
                * tarifa.getMultiplicadorPrecio();
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
