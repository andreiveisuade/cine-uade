package ar.uade.cine.dto.promociones;

import java.util.List;

public record PedidoPromocionDTO(String nombre, String tipo, Double porcentaje, Double monto,
                              Integer lleva, Integer paga, String vigenciaDesde, String vigenciaHasta,
                              List<String> diasSemana, String horaDesde, String horaHasta,
                              List<String> mediosPago) {
}
