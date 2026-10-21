package ar.uade.cine.swing.pantallas.salas;

import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.pantallas.acciones.AccionDeFila;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.Optional;

// Borrar la sala elegida. ConcreteCommand de AccionDeFila; Receiver: ApiSalas.
record BorrarSala(ApiSalas api) implements AccionDeFila<Sala> {

    @Override
    public String texto() {
        return "Borrar";
    }

    @Override
    public Optional<Pregunta> pregunta(Sala s) {
        return Optional.of(new Pregunta("¿Borrar " + s.nombre() + "?", "Sí, borrar"));
    }

    @Override
    public void ejecutar(Sala s) {
        api.eliminarSala(s.id());
    }

    @Override
    public String hecho(Sala s) {
        return "Sala borrada";
    }
}
