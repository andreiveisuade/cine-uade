package ar.uade.cine.dto.promociones;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record PedidoPromocionDTO(String nombre, String tipo, Double porcentaje, Double monto,
                                 Integer lleva, Integer paga,
                                 @NotBlank(message = "Falta el inicio de la vigencia") String vigenciaDesde,
                                 @NotBlank(message = "Falta el fin de la vigencia") String vigenciaHasta,
                                 List<String> diasSemana, String horaDesde, String horaHasta,
                                 List<String> mediosPago) {
}
