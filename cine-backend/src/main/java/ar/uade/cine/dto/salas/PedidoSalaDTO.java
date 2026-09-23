package ar.uade.cine.dto.salas;

import java.util.List;

public record PedidoSalaDTO(String nombre, String tipo, List<Integer> butacasPorFila,
                            List<String> codigosVip, List<String> codigosPareja,
                            List<String> codigosAccesibles, Integer minutosLimpieza) {
}
