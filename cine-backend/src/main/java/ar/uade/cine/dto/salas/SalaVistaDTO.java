package ar.uade.cine.dto.salas;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalaVistaDTO(int id, String nombre, String tipo, List<Integer> butacasPorFila,
                           int filas, int capacidadSala, int minutosLimpieza,
                           List<AsientoVistaDTO> asientos) {
}
