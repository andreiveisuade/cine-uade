package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.pantallas.acciones.AccionDeFila;
import ar.uade.cine.swing.pantallas.acciones.Pregunta;

import java.util.Optional;

// Sacar de la carta o reponer el producto elegido. ConcreteCommand de AccionDeFila; Receiver: ApiCandy.
record CambiarDisponibilidadProducto(ApiCandy api) implements AccionDeFila<Producto> {

    @Override
    public String texto() {
        return "Sacar de la carta";
    }

    @Override
    public String texto(Producto p) {
        return p.disponible() ? "Sacar de la carta" : "Reponer";
    }

    @Override
    public Optional<Pregunta> pregunta(Producto p) {
        if (!p.disponible()) return Optional.empty();
        return Optional.of(new Pregunta("¿Sacar " + p.nombre() + " de la carta? Deja de venderse en el "
                + "mostrador.", "Sí, sacar"));
    }

    @Override
    public void ejecutar(Producto p) {
        api.cambiarDisponibilidadCandy(p.id(), !p.disponible());
    }

    @Override
    public String hecho(Producto p) {
        return p.nombre() + (p.disponible() ? " salió de la carta" : " volvió a la carta");
    }
}
