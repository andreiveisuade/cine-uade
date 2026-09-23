package ar.uade.cine.model.cartelera;

/**
 * En qué terminó una corrida del importador. No hay PENDIENTE: la corrida arranca en el
 * momento en que se la pide.
 */
public enum EstadoImportacion {

    EN_CURSO,

    /** Aun con cero películas: que no haya nada nuevo en TMDB no es un error. */
    TERMINADA,

    /** El motivo va en {@code detalle}. Pudo haber cargado películas antes de cortar: el buzón dice qué entró. */
    FALLIDA
}
