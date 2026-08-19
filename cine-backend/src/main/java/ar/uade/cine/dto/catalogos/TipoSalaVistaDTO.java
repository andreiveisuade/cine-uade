package ar.uade.cine.dto.catalogos;

/** Con {@code soportaTresD} el front puede avisar de R8 antes de mandar la función. */
public record TipoSalaVistaDTO(String nombre, double multiplicador, boolean soportaTresD) {
}
