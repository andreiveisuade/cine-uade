package ar.uade.cine.controller.vistas;

import org.springframework.stereotype.Component;

import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.promociones.PromocionMontoFijo;
import ar.uade.cine.model.promociones.PromocionNxM;
import ar.uade.cine.model.promociones.PromocionPorcentaje;
import ar.uade.cine.dto.promociones.PromocionVistaDTO;

@Component
public class VistasPromociones {

    public PromocionVistaDTO promocion(Promocion p) {
        return new PromocionVistaDTO(p.getId(), p.getNombre(), p.getTipo().name(),
                p instanceof PromocionPorcentaje pp ? pp.getPorcentaje() : null,
                p instanceof PromocionMontoFijo pm ? pm.getMonto().aPesos() : null,
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
