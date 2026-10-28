package ar.uade.cine.dto.ventas;

// Una entrada de ReservaVistaDTO: butaca, tarifa por constante, si pide carnet y precio ya calculado.
public record EntradaVistaDTO(int asientoId, String codigo, String tarifa, boolean requiereAcreditacion,
                              double precio) {
}
