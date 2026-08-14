package ar.uade.cine.dto.catalogos;

/**
 * El multiplicador va porque el front muestra el precio de cada butaca antes de reservar:
 * sin esto tendría que repetir los factores de su lado, y serían dos fuentes de verdad
 * para lo mismo.
 */
public record TarifaVistaDTO(String nombre, double multiplicador, boolean requiereAcreditacion) {
}
