package ar.uade.cine.model.cartelera;

/**
 * Si la película entró al catálogo: lo importado de TMDB espera en un buzón hasta que el
 * encargado decide. Es otro eje que {@code enCartelera} (si se está dando): juntarlos
 * confundiría «no la miré» con «la saqué de cartelera».
 */
public enum EstadoRevision {

    /** No se puede programar ni exhibir. */
    PENDIENTE,

    CONFIRMADA,

    /** No se borra: la próxima importación la traería de nuevo. */
    DESCARTADA
}
