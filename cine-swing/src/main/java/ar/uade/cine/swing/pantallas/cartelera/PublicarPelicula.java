package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.dto.cartelera.PedidoPelicula;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.pantallas.acciones.AccionDeFila;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.Optional;

// Publicar o despublicar las películas elegidas. ConcreteCommand de AccionDeFila; Receiver: ApiCartelera.
record PublicarPelicula(ApiCartelera api, boolean publicar) implements AccionDeFila<Pelicula> {

    @Override
    public String texto() {
        return publicar ? "Publicar" : "Despublicar";
    }

    // Con varias elegidas, cada botón actúa solo sobre las que cambian: no alterna a ciegas.
    @Override
    public boolean habilitada(Pelicula p) {
        return p.enCartelera() != publicar;
    }

    // Despublicar la baja de la cartelera del cliente aunque tenga funciones: se pregunta. Publicar no.
    @Override
    public Optional<Pregunta> pregunta(Pelicula p) {
        if (publicar) return Optional.empty();
        return Optional.of(new Pregunta("¿Despublicar " + p.titulo() + "? Deja de verse en la cartelera "
                + "aunque tenga funciones.", "Sí, despublicar"));
    }

    @Override
    public void ejecutar(Pelicula p) {
        api.actualizarPelicula(p.id(), PedidoPelicula.soloPublicacion(publicar));
    }

    @Override
    public String hecho(Pelicula p) {
        return publicar ? "Publicada" : "Despublicada";
    }
}
