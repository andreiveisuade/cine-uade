package ar.uade.cine.swing.api.dto.salas;

import java.util.List;

public record PedidoSala(String nombre, String tipo, List<Integer> butacasPorFila, List<String> codigosVip,
                         List<String> codigosPareja, List<String> codigosAccesibles, Integer minutosLimpieza) {
}
