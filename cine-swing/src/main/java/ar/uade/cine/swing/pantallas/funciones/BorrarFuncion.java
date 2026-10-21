package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.pantallas.acciones.AccionDeFila;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.Optional;

import static ar.uade.cine.swing.comun.Formato.dia;
import static ar.uade.cine.swing.comun.Formato.hora;

// Borrar la función elegida. ConcreteCommand de AccionDeFila; Receiver: ApiFunciones.
record BorrarFuncion(ApiFunciones api) implements AccionDeFila<Funcion> {

    @Override
    public String texto() {
        return "Borrar";
    }

    @Override
    public Optional<Pregunta> pregunta(Funcion f) {
        return Optional.of(new Pregunta("¿Borrar la función de " + f.pelicula().titulo() + " del " + dia(f.inicio())
                + " " + hora(f.inicio()) + "?", "Sí, borrar"));
    }

    @Override
    public void ejecutar(Funcion f) {
        api.eliminarFuncion(f.id());
    }

    @Override
    public String hecho(Funcion f) {
        return "Función borrada";
    }
}
