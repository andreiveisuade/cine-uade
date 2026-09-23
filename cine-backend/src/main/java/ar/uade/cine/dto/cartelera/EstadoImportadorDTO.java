package ar.uade.cine.dto.cartelera;

/** Para que la pantalla avise que el importador no está antes de que alguien lo dispare. */
public record EstadoImportadorDTO(boolean disponible, String detalle) {
}
