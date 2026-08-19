package ar.uade.cine.dto.promociones;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Una promoción con el beneficio expuesto en el campo que corresponde a su tipo: las otras
 * dos columnas viajan en null y Jackson las deja afuera.
 *
 * <p>Las condiciones vacías viajan como listas vacías, que es como las lee el gestor:
 * sin días significa todos los días, sin medios significa cualquiera.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromocionVistaDTO(int id, String nombre, String tipo, Double porcentaje, Double monto,
                             Integer lleva, Integer paga, String vigenciaDesde, String vigenciaHasta,
                             List<String> diasSemana, String horaDesde, String horaHasta,
                             List<String> mediosPago, boolean activa) {
}
