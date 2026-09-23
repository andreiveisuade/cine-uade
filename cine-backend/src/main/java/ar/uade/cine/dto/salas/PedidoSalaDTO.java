package ar.uade.cine.dto.salas;

import java.util.List;

/**
 * {@code butacasPorFila} [8, 10, 12] es fila A con 8, B con 10, C con 12. Las no estándar van
 * por código en las tres listas.
 */
public record PedidoSalaDTO(String nombre, String tipo, List<Integer> butacasPorFila,
                            List<String> codigosVip, List<String> codigosPareja,
                            List<String> codigosAccesibles, Integer minutosLimpieza) {
}
