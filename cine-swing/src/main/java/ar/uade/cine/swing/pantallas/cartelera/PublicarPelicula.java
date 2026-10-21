package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.dto.cartelera.PedidoPelicula;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.pantallas.acciones.AccionDeFila;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.Optional;

// Publicar o despublicar la película elegida. ConcreteCommand de AccionDeFila; Receiver: ApiCartelera.
record PublicarPelicula(ApiCartelera api) implements AccionDeFila<Pelicula> {

    @Override
    public String texto() {
        return "Publicar / despublicar";
    }

    // Despublicar la baja de la cartelera del cliente aunque tenga funciones: se pregunta. Publicar no.
    @Override
    public Optional<Pregunta> pregunta(Pelicula p) {
        if (!p.enCartelera()) return Optional.empty();
        return Optional.of(new Pregunta("¿Despublicar " + p.titulo() + "? Deja de verse en la cartelera "
                + "aunque tenga funciones.", "Sí, despublicar"));
    }

    @Override
    public void ejecutar(Pelicula p) {
        api.actualizarPelicula(p.id(), PedidoPelicula.soloPublicacion(!p.enCartelera()));
    }

    @Override
    public String hecho(Pelicula p) {
        return p.enCartelera() ? "Despublicada" : "Publicada";
    }
}
