package ar.uade.cine.dominio.ventas;

/**
 * Una butaca vendida dentro de una reserva. Es un value object: no tiene identidad
 * propia ni DAO, se guarda y se lee siempre junto con su reserva. Por eso es un record
 * y no una interfaz con implementación: no hay dos formas posibles de ser una entrada.
 *
 * @param asientoId     butaca de la sala que ocupa
 * @param codigoAsiento copiado acá para que el ticket no dependa de otra consulta
 * @param precio        lo que se cobró por esta butaca, congelado al reservar: si mañana
 *                      sube el precio de la función, el ticket ya emitido tiene que
 *                      seguir diciendo lo que se pagó
 */
public record Entrada(int asientoId, String codigoAsiento, double precio) {

    @Override
    public String toString() {
        return codigoAsiento;
    }
}
