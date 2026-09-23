package ar.uade.cine.dto.promociones;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Solo el campo de beneficio de su tipo viaja; los otros van en null y se omiten. Sin días es
 * todos los días; sin medios, cualquiera.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromocionVistaDTO(int id, String nombre, String tipo, Double porcentaje, Double monto,
                             Integer lleva, Integer paga, String vigenciaDesde, String vigenciaHasta,
                             List<String> diasSemana, String horaDesde, String horaHasta,
                             List<String> mediosPago, boolean activa) {
}
