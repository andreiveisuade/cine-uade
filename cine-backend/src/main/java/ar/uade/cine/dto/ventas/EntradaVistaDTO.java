package ar.uade.cine.dto.ventas;

/** {@code tarifa} viaja para que el acomodador sepa si pedir un carnet. */
public record EntradaVistaDTO(int asientoId, String codigo, String tarifa, double precio) {
}
