package ar.uade.cine.dto.salas;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code estado} es de la butaca y vale para todas las funciones; {@code ocupado}
 * es de esta función. Van separados porque el front los pinta distinto. Sin función
 * de por medio, ocupado y precio no aplican y no se mandan.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AsientoVistaDTO(int id, int salaId, int fila, int numero, String codigo,
                           String tipo, String estado, Boolean ocupado, Double precio) {
}
