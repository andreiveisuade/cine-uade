package ar.uade.cine.model.cartelera;

/**
 * Si el encargado ya decidió qué hacer con una película, y qué decidió.
 *
 * <p>Existe porque el importador de TMDB trae la cartelera entera de Argentina —dieciocho
 * títulos y subiendo— y que todos entren derecho al catálogo hace que el catálogo deje de
 * ser una decisión del cine para pasar a ser una copia de lo que pasa afuera. La película
 * importada cae en un buzón y espera; recién cuando alguien la mira pasa a existir para el
 * resto del sistema.
 *
 * <p>Es un eje distinto de {@code enCartelera}, y por eso son dos campos y no uno.
 * {@code enCartelera} es <em>si se está dando</em>, y cambia todas las semanas. Este dice
 * <em>si la película entró al catálogo</em>, que se responde una sola vez en la vida de
 * cada una. Colapsarlos haría indistinguible «todavía no la mire» de «la saque de
 * cartelera la semana pasada», que es justo la pantalla que hay que poder dibujar.
 */
public enum EstadoRevision {

    /** La trajo el importador y nadie la miró todavía. No se puede programar ni exhibir. */
    PENDIENTE,

    /** El encargado la aceptó, o la cargó él mismo: es parte del catálogo del cine. */
    CONFIRMADA,

    /**
     * El encargado no la quiere. No se borra: si se borrara, la próxima corrida del
     * importador la traería de nuevo y habría que descartarla otra vez, para siempre.
     */
    DESCARTADA
}
