package ar.uade.cine.dto.ventas;

// Una entrada dentro de ReservaVistaDTO: la butaca, su tarifa por constante y el precio ya calculado.
public record EntradaVistaDTO(int asientoId, String codigo, String tarifa, double precio) {
}
