package ar.uade.cine.dto.promociones;

import java.util.List;

/**
 * Un solo pedido para los tres tipos, con las columnas del beneficio en null salvo
 * la que corresponde: es la misma forma que tiene la tabla, y evita tres endpoints
 * que se diferencian en un campo.
 */
public record PedidoPromocionDTO(String nombre, String tipo, Double porcentaje, Double monto,
                              Integer lleva, Integer paga, String vigenciaDesde, String vigenciaHasta,
                              List<String> diasSemana, String horaDesde, String horaHasta,
                              List<String> mediosPago) {
}
