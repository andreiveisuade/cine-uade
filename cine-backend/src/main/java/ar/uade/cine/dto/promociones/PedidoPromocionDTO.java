package ar.uade.cine.dto.promociones;

import java.util.List;

/**
 * Un pedido para los tres tipos: solo la columna del beneficio que corresponde viene cargada,
 * igual que en la tabla.
 */
public record PedidoPromocionDTO(String nombre, String tipo, Double porcentaje, Double monto,
                              Integer lleva, Integer paga, String vigenciaDesde, String vigenciaHasta,
                              List<String> diasSemana, String horaDesde, String horaHasta,
                              List<String> mediosPago) {
}
