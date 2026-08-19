package ar.uade.cine.dto.ventas;

/** tarifa viaja para que el acomodador sepa si tiene que pedir un carnet. */
public record EntradaVistaDTO(int asientoId, String codigo, String tarifa, double precio) {
}
