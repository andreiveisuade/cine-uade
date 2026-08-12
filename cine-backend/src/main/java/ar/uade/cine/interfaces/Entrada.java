package ar.uade.cine.interfaces;

/**
 * Una butaca vendida dentro de una reserva. No tiene DAO propio: se guarda y se lee
 * siempre junto con su reserva.
 */
public interface Entrada {

    int getAsientoId();

    /** Guardado junto con la entrada para que el ticket no dependa de otra consulta. */
    String getCodigoAsiento();
}
