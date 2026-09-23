package ar.uade.cine.dto.salas;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AsientoVistaDTO(int id, int salaId, int fila, int numero, String codigo,
                           String tipo, String estado, Boolean ocupado, Double precio) {
}
