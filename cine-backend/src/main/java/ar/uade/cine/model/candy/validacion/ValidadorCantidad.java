package ar.uade.cine.model.candy.validacion;

import ar.uade.cine.model.validacion.Regla;

// Cuántas unidades de un producto lleva un renglón, de 1 a 20; la regla que comparten la venta y el combo.
public final class ValidadorCantidad {

    // Veinte de lo mismo ya es error de tipeo; sin tope, un 2000 por un 20 se cobraba igual.
    public static final int MAXIMA = 20;

    private ValidadorCantidad() {
    }

    // Integer: el null tiene su propio mensaje, no un 500. El producto va por nombre para saber cuál está mal.
    public static int valida(Integer cantidad, String producto) {
        return Regla.numero(cantidad).obligatorio("Falta la cantidad de " + producto)
                .mayorQueCero("La cantidad de " + producto + " tiene que ser mayor a cero")
                .entre(1, MAXIMA, "La cantidad de " + producto + " tiene que ser como máximo " + MAXIMA)
                .valor();
    }
}
