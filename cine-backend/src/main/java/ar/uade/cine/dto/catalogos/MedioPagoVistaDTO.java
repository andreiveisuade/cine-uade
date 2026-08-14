package ar.uade.cine.dto.catalogos;

/** Con {@code requiereAutorizacion} el front pide el código de R11 solo cuando hace falta. */
public record MedioPagoVistaDTO(String nombre, boolean requiereAutorizacion) {
}
