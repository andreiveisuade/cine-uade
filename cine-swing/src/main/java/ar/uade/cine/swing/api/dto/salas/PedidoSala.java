package ar.uade.cine.swing.api.dto.salas;

import java.util.List;

// Lo que Swing manda al dar de alta una sala: butacas por fila y los códigos de las especiales.
public record PedidoSala(String nombre, String tipo, List<Integer> butacasPorFila, List<String> codigosVip,
                         List<String> codigosPareja, List<String> codigosAccesibles, Integer minutosLimpieza) {
}
