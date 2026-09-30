package ar.uade.cine.model.ventas.validacion;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.validacion.Regla;
import ar.uade.cine.model.ventas.MedioPago;

// Qué cumple un cobro al registrarse: medio de pago e importes que cierran; guardas de Regla que llama Pago.
// Los importes no los tipea nadie, salen de la reserva y de la promoción: si no cierran es
// un error del cálculo, y es mejor frenarlo acá que guardar un monto negativo en la caja.
public final class ValidadorPago {

    private ValidadorPago() {
    }

    public static void validar(Dinero subtotal, Dinero descuento, MedioPago medio) {
        Regla.objeto(medio).obligatorio("Falta el medio de pago");
        Regla.numero(subtotal.centavos()).noNegativo("El subtotal del pago tiene que ser mayor o igual a cero");
        Regla.numero(descuento.centavos())
                .noNegativo("El descuento tiene que ser mayor o igual a cero")
                .entre(0L, subtotal.centavos(), "El descuento tiene que ser menor o igual al subtotal");
    }
}
