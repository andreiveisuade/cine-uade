package ar.uade.cine.dto.promociones;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// Una promoción de /api/promociones/vigentes, sin id ni estado: lo que el cliente necesita para aprovecharla.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromocionPublicaVistaDTO(String nombre, String tipo, String beneficio, String vigenciaDesde,
                                       String vigenciaHasta, List<String> diasSemana, String horaDesde,
                                       String horaHasta, List<String> mediosPago, List<String> tarifasExcluidas,
                                       String imagenUrl) {
}
