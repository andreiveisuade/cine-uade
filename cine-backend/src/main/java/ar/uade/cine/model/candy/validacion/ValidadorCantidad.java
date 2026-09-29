package ar.uade.cine.model.candy.validacion;

import ar.uade.cine.model.validacion.Regla;

// Cuántas unidades de un producto lleva un renglón, de 1 a 20; la regla que comparten la venta y el combo.
// Antes ItemCompra e ItemCombo repetían el mismo chequeo letra por letra, y un cambio en uno no llegaba
// al otro. Es la misma cantidad en los dos: el combo la declara y la venta la multiplica.
public final class ValidadorCantidad {

    // Decisión de Andrei: veinte de lo mismo en un renglón ya es un error de tipeo, no un pedido de
    // mostrador. Sin tope, un 2000 por un 20 se cobraba igual.
    public static final int MAXIMA = 20;

    private ValidadorCantidad() {
    }

    // Integer porque viene del pedido: el null tiene su propio mensaje en vez de un 500 por el unboxing.
    // El producto va por nombre en el mensaje: en un pedido de varios, dice cuál está mal.
    public static int valida(Integer cantidad, String producto) {
        return Regla.numero(cantidad).obligatorio("Falta la cantidad de " + producto)
                .mayorQueCero("La cantidad de " + producto + " tiene que ser mayor a cero")
                .entre(1, MAXIMA, "La cantidad de " + producto + " tiene que ser como máximo " + MAXIMA)
                .valor();
    }
}
