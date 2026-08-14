package ar.uade.cine.servicio.ventas;

import ar.uade.cine.dominio.dinero.Dinero;
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
 *
 * <p>Ya no redondea. Antes tenía un {@code redondear} propio —copiado además en
 * {@code PromocionPorcentaje}— porque multiplicar {@code double} arrastraba error:
 * 5250.50 × 1.3 daba 6825.650000000001. Desde que la plata es {@link Dinero}, el redondeo
 * al centavo pasa una sola vez y adentro del tipo, así que acá solo quedan las tres
 * multiplicaciones que son la regla de precio.
 */
public class CalculadoraPrecio {

    public Dinero precioDe(Funcion funcion, Sala sala, Asiento asiento, TipoTarifa tarifa) {
        return funcion.getPrecio()
                .por(sala.getTipo().getMultiplicadorPrecio())
                .por(asiento.getTipo().getMultiplicadorPrecio())
                .por(tarifa.getMultiplicadorPrecio());
    }

    /**
     * Lo que sale la butaca más barata de esa función: la estándar, que no tiene recargo
     * por tipo. Es el "desde $" de la cartelera.
     */
    public Dinero precioBaseEnSala(Funcion funcion, Sala sala) {
        return funcion.getPrecio().por(sala.getTipo().getMultiplicadorPrecio());
    }
}
