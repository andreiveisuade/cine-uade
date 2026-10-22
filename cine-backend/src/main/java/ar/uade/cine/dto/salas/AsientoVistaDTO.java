package ar.uade.cine.dto.salas;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

// Una butaca de una sala; ocupado y precios solo vienen en el mapa de una función, afuera se omiten.
// precio es el de la tarifa general; precios, el de cada tarifa, para que el cliente no los recalcule.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AsientoVistaDTO(int id, int salaId, int fila, int numero, String codigo,
                              String tipo, String estado, Boolean ocupado, Double precio,
                              Map<String, Double> precios) {
}
