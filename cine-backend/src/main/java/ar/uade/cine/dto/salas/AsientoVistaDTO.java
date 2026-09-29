package ar.uade.cine.dto.salas;

import com.fasterxml.jackson.annotation.JsonInclude;

// Una butaca de una sala; ocupado y precio solo vienen en el mapa de una función, afuera se omiten.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AsientoVistaDTO(int id, int salaId, int fila, int numero, String codigo,
                           String tipo, String estado, Boolean ocupado, Double precio) {
}
