package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.pantallas.acciones.AccionDeFila;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.Optional;

// Borrar la película elegida. ConcreteCommand de AccionDeFila; Receiver: ApiCartelera.
record BorrarPelicula(ApiCartelera api) implements AccionDeFila<Pelicula> {

    @Override
    public boolean destructiva() {
        return true;
    }

    @Override
    public String texto() {
        return "Borrar";
    }

    @Override
    public Optional<Pregunta> pregunta(Pelicula p) {
        return Optional.of(new Pregunta("¿Borrar " + p.titulo() + "?", "Sí, borrar"));
    }

    @Override
    public void ejecutar(Pelicula p) {
        api.eliminarPelicula(p.id());
    }

    @Override
    public String hecho(Pelicula p) {
        return "Película borrada";
    }
}
