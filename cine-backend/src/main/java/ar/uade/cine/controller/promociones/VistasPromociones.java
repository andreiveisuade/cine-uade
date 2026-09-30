package ar.uade.cine.controller.promociones;

import org.springframework.stereotype.Component;

import java.time.LocalTime;

import ar.uade.cine.model.promociones.ParametrosPromocion;
import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.tiempo.FranjaHoraria;
import ar.uade.cine.dto.promociones.PromocionVistaDTO;

// Arma un único JSON para las tres subclases de Promocion, con null en lo que no aplica; Assembler.
@Component
public class VistasPromociones {

    public PromocionVistaDTO promocion(Promocion p) {
        // Sin instanceof: cada subclase describe sus parámetros (polimorfismo en Promocion.getParametros).
        ParametrosPromocion parametros = p.getParametros();
        FranjaHoraria franja = p.getFranja();
        return new PromocionVistaDTO(p.getId(), p.getNombre(), p.getTipo().name(),
                parametros.porcentaje(), parametros.monto(), parametros.lleva(), parametros.paga(),
                p.getVigencia().desde().toString(), p.getVigencia().hasta().toString(),
                p.getDiasSemana().stream().map(Enum::name).toList(),
                texto(franja.desde()), texto(franja.hasta()),
                p.getMediosPago().stream().map(Enum::name).toList(),
                p.estaActiva());
    }

    private static String texto(LocalTime hora) {
        return hora == null ? null : hora.toString();
    }
}
