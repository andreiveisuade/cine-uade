package ar.uade.cine.dto.salas;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// Una sala de /api/salas; butacasPorFila sale de contar sus butacas y asientos solo viene en el detalle.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalaVistaDTO(int id, String nombre, String tipo, List<Integer> butacasPorFila,
                           int filas, int capacidadSala, int minutosLimpieza,
                           List<AsientoVistaDTO> asientos) {
}
