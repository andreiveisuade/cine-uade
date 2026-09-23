package ar.uade.cine.dto.catalogos;

/** El multiplicador viaja para que el front muestre precios sin repetir los factores. */
public record TarifaVistaDTO(String nombre, double multiplicador, boolean requiereAcreditacion) {
}
