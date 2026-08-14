package ar.uade.cine.dominio.cartelera;

/**
 * En qué terminó una corrida del importador de cartelera.
 *
 * <p>Son tres y no cuatro: no hay PENDIENTE. La corrida arranca en el mismo momento en que
 * se la pide —el encargado aprieta el botón y el backend le habla al importador ahí mismo—,
 * así que nunca hay una importación esperando su turno. Un estado que ninguna fila puede
 * tener es una rama muerta en cada {@code switch} que lo mire.
 */
public enum EstadoImportacion {

    /** Se la pidió y todavía no volvió. */
    EN_CURSO,

    /**
     * El importador respondió. Puede haber traído cero películas y seguir siendo TERMINADA:
     * que no haya nada nuevo en TMDB no es un error, es la respuesta.
     */
    TERMINADA,

    /**
     * No se pudo. El importador no estaba levantado, TMDB rechazó el token, o la corrida no
     * respondió a tiempo. El motivo queda en {@code detalle}, con el texto que se le muestra
     * al encargado.
     *
     * <p>Una corrida FALLIDA igual pudo haber cargado películas antes de cortarse: el
     * importador escribe una por una contra la API. El buzón es siempre la verdad de qué
     * entró; esto solo dice cómo terminó el pedido.
     */
    FALLIDA
}
