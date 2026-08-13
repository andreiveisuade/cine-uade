package ar.uade.cine.dominio.funciones;

/**
 * Cómo se escucha la copia que se proyecta en esta función. La misma película en la
 * misma sala puede darse doblada a las 15:00 y subtitulada a las 22:00: es propiedad
 * de la función, no de la película.
 *
 * <p>No es el idioma hablado: ese es un dato de catálogo de la película.
 */
public enum Version {
    DOBLADA,
    SUBTITULADA
}
