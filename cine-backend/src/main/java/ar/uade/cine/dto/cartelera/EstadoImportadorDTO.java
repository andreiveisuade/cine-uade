package ar.uade.cine.dto.cartelera;

/**
 * Si el importador está levantado, para que la pantalla avise antes de que alguien apriete
 * el botón y espere una respuesta que no va a llegar.
 *
 * @param detalle qué le pasa, en el mismo castellano en que se muestra
 */
public record EstadoImportadorDTO(boolean disponible, String detalle) {
}
