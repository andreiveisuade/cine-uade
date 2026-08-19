package ar.uade.cine.dto.catalogos;

/** La clasificación con su edad mínima, para que el front la muestre sin repetir la tabla. */
public record ClasificacionVistaDTO(String nombre, int edadMinima) {
}
