package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.ApiProgramaciones;
import ar.uade.cine.swing.api.dto.programaciones.Programacion;
import ar.uade.cine.swing.pantallas.acciones.AccionDeFila;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.Optional;

// Dar de baja o reactivar la grilla elegida. ConcreteCommand de AccionDeFila; Receiver: ApiProgramaciones.
record CambiarActivacionGrilla(ApiProgramaciones api) implements AccionDeFila<Programacion> {

    @Override
    public String texto() {
        return "Dar de baja";
    }

    @Override
    public String texto(Programacion p) {
        return p.activa() ? "Dar de baja" : "Reactivar";
    }

    @Override
    public Optional<Pregunta> pregunta(Programacion p) {
        if (!p.activa()) return Optional.empty();
        return Optional.of(new Pregunta("¿Dar de baja esta grilla? Deja de generar funciones; las que ya generó "
                + "quedan.", "Sí, dar de baja"));
    }

    @Override
    public void ejecutar(Programacion p) {
        api.cambiarActivacionProgramacion(p.id(), !p.activa());
    }

    @Override
    public String hecho(Programacion p) {
        return p.activa() ? "Grilla dada de baja" : "Grilla reactivada";
    }
}
