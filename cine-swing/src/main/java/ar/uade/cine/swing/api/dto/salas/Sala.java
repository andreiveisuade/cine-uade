package ar.uade.cine.swing.api.dto.salas;

import java.util.List;

// `asientos` solo viene en el detalle (`GET /api/salas/{id}`).
public record Sala(int id, String nombre, String tipo, List<Integer> butacasPorFila, int filas, int capacidadSala,
                   int minutosLimpieza, List<Asiento> asientos) {
}
