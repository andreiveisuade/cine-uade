package ar.uade.cine.dto.salas;

import java.util.List;

/**
 * La distribución llega como lista —[8, 10, 12] es fila A con 8, B con 10 y C con 12— y
 * las butacas que no son estándar vienen por código en tres listas, que es como el gestor
 * espera el mapa de especiales.
 */
public record PedidoSalaDTO(String nombre, String tipo, List<Integer> butacasPorFila,
                            List<String> codigosVip, List<String> codigosPareja,
                            List<String> codigosAccesibles, Integer minutosLimpieza) {
}
