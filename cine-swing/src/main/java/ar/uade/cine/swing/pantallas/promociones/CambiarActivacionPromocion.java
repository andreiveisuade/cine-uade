package ar.uade.cine.swing.pantallas.promociones;

import ar.uade.cine.swing.api.ApiPromociones;
import ar.uade.cine.swing.api.dto.promociones.Promocion;
import ar.uade.cine.swing.pantallas.acciones.AccionDeFila;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.Optional;

// Dar de baja o reactivar la promoción elegida. ConcreteCommand de AccionDeFila; Receiver: ApiPromociones.
record CambiarActivacionPromocion(ApiPromociones api) implements AccionDeFila<Promocion> {

    @Override
    public String texto() {
        return "Dar de baja";
    }

    @Override
    public String texto(Promocion p) {
        return p.activa() ? "Dar de baja" : "Reactivar";
    }

    @Override
    public Optional<Pregunta> pregunta(Promocion p) {
        if (!p.activa()) return Optional.empty();
        return Optional.of(new Pregunta("¿Dar de baja " + p.nombre() + "? Deja de aplicarse en los cobros.",
                "Sí, dar de baja"));
    }

    @Override
    public void ejecutar(Promocion p) {
        api.cambiarActivacionPromocion(p.id(), !p.activa());
    }

    @Override
    public String hecho(Promocion p) {
        return p.nombre() + (p.activa() ? " dada de baja" : " reactivada");
    }
}
