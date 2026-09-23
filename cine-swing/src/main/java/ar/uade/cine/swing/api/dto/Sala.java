package ar.uade.cine.swing.api.dto;

import java.util.List;

public record Sala(int id, String nombre, String tipo, List<Integer> butacasPorFila, int filas, int capacidadSala,
                   int minutosLimpieza) {
}
