package ar.uade.cine.service.ventas;

import org.springframework.stereotype.Service;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.ventas.TipoTarifa;

/**
 * Precio <strong>de lista</strong> de una butaca: base × sala × tipo de butaca × tarifa.
 * Las promociones no pasan por acá: se calculan sobre el total al cobrar, porque un 2x1
 * no es un factor por butaca.
 */
@Service
public class CalculadoraPrecio {

    public Dinero precioDe(Funcion funcion, Sala sala, Asiento asiento, TipoTarifa tarifa) {
        return funcion.getPrecio()
                .por(sala.getTipo().getMultiplicadorPrecio())
                .por(asiento.getTipo().getMultiplicadorPrecio())
                .por(tarifa.getMultiplicadorPrecio());
    }

    /** La estándar, sin recargo por tipo: el "desde $" de la cartelera. */
    public Dinero precioBaseEnSala(Funcion funcion, Sala sala) {
        return funcion.getPrecio().por(sala.getTipo().getMultiplicadorPrecio());
    }
}
