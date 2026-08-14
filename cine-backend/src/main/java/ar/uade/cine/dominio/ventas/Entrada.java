package ar.uade.cine.dominio.ventas;

import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Una butaca vendida dentro de una reserva. Es un value object: no tiene identidad
 * propia ni DAO, se guarda y se lee siempre junto con su reserva. Por eso es un record
 * y no una interfaz con implementación: no hay dos formas posibles de ser una entrada.
 *
 * @param asientoId     butaca de la sala que ocupa
 * @param codigoAsiento copiado acá para que el ticket no dependa de otra consulta
 * @param tarifa        quién la compra. Es por persona, y por eso está acá y no en la
 *                      reserva: en una reserva de cuatro puede haber dos generales, un
 *                      menor y un jubilado
 * @param precio        el precio <strong>de lista</strong> de esta butaca, ya con la
 *                      tarifa aplicada, congelado al reservar: si mañana sube el precio
 *                      de la función, el ticket ya emitido tiene que seguir diciendo lo
 *                      que se pagó. Ninguna promoción lo toca: el descuento se calcula
 *                      sobre el total y vive en el {@link Pago}
 */
public record Entrada(int asientoId, String codigoAsiento, TipoTarifa tarifa, Dinero precio) {

    @Override
    public String toString() {
        return tarifa == TipoTarifa.GENERAL ? codigoAsiento : codigoAsiento + " (" + tarifa + ")";
    }
}
