package ar.uade.cine.swing.api.dto.promociones;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

// Los campos de beneficio que no aplican al tipo viajan en null explícito, como pide el contrato.
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PedidoPromocion(String nombre, String tipo, Double porcentaje, Double monto, Integer lleva,
                              Integer paga, String vigenciaDesde, String vigenciaHasta, List<String> diasSemana,
                              String horaDesde, String horaHasta, List<String> mediosPago) {
}
