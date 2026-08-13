package ar.uade.cine.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dominio.promociones.Promocion;
import ar.uade.cine.dominio.promociones.PromocionMontoFijo;
import ar.uade.cine.dominio.promociones.PromocionNxM;
import ar.uade.cine.dominio.promociones.PromocionPorcentaje;

/**
 * Las promociones, en la forma que espera el front.
 *
 * <p>Es el único lugar donde se pregunta de qué tipo es una promoción, y con un motivo:
 * el JSON tiene que mostrar el beneficio, y cada tipo lo expresa con un campo distinto.
 * El cálculo del descuento sigue siendo polimórfico y no pasa por acá.
 */
public class VistasPromociones {

    /**
     * Las condiciones vacías viajan como listas vacías, que es como las lee el gestor:
     * sin días significa todos los días, sin medios significa cualquiera.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PromocionVista(int id, String nombre, String tipo, Double porcentaje, Double monto,
                                 Integer lleva, Integer paga, String vigenciaDesde, String vigenciaHasta,
                                 List<String> diasSemana, String horaDesde, String horaHasta,
                                 List<String> mediosPago, boolean activa) {
    }

    public PromocionVista promocion(Promocion p) {
        return new PromocionVista(p.getId(), p.getNombre(), p.getTipo().name(),
                p instanceof PromocionPorcentaje pp ? pp.getPorcentaje() : null,
                p instanceof PromocionMontoFijo pm ? pm.getMonto() : null,
                p instanceof PromocionNxM pn ? pn.getLleva() : null,
                p instanceof PromocionNxM pn ? pn.getPaga() : null,
                p.getVigenciaDesde().toString(), p.getVigenciaHasta().toString(),
                p.getDiasSemana().stream().map(Enum::name).toList(),
                p.getHoraDesde() == null ? null : p.getHoraDesde().toString(),
                p.getHoraHasta() == null ? null : p.getHoraHasta().toString(),
                p.getMediosPago().stream().map(Enum::name).toList(),
                p.estaActiva());
    }
}
