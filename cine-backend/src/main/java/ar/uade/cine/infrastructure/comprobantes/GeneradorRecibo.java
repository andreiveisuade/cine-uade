package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;

/**
 * Emite el recibo de caja: el papel que se entrega cuando se cobra en efectivo por la
 * boletería del cine.
 *
 * <p>Sale <strong>solo con el efectivo</strong>, y el criterio es el mismo que ya usa R11
 * al revés. Un pago electrónico deja rastro afuera del cine —el cupón del posnet, el
 * comprobante de MercadoPago— y ese rastro es el código de autorización que queda guardado
 * en el {@link Pago}. El efectivo no deja ninguno: si el cine no imprime nada, el cliente
 * se va sin constancia de haber pagado y la caja no tiene contra qué cuadrar. Por eso el
 * discriminador no es una lista de medios sino {@code medio.requiereAutorizacion()}:
 * agregar un medio nuevo no obliga a acordarse de este archivo.
 *
 * <p>Va aparte de {@link GeneradorTicket} porque son dos comprobantes de dos momentos
 * distintos: el ticket dice qué butacas son y se emite al reservar; el recibo dice cuánto
 * entró en la caja y se emite al cobrar, que puede ser media hora después.
 */
public interface GeneradorRecibo {

    void emitir(Pago pago, Reserva reserva);
}
