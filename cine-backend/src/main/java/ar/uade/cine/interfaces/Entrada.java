package ar.uade.cine.interfaces;

/**
 * Una butaca vendida dentro de una reserva. No tiene DAO propio: se guarda y se lee
 * siempre junto con su reserva.
 */
public interface Entrada {

    int getAsientoId();

    /** Guardado junto con la entrada para que el ticket no dependa de otra consulta. */
    String getCodigoAsiento();

    /**
     * Lo que se cobró por esta butaca. Se congela al reservar: si mañana sube el precio
     * de la función, el ticket ya emitido tiene que seguir diciendo lo que se pagó.
     */
    double getPrecio();
}
