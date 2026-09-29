package ar.uade.cine.model.validacion;

import java.util.Collection;
import java.util.Objects;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Guardas de una colección: con elementos, sin nulos y con tope; Fluent Interface desde Regla.
public final class ReglaDeLista<C extends Collection<?>> {

    private final C valor;

    ReglaDeLista(C valor) {
        this.valor = valor;
    }

    // Una lista vacía también falta: una reserva sin butacas es un pedido vacío.
    public ReglaDeLista<C> noVacia(String mensaje) {
        if (valor == null || valor.isEmpty()) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    // `[null]` llega del JSON como una lista con un elemento: sin esto, el null explotaría más adelante.
    // Por stream y no por contains(null): List.of(...) tira NullPointerException si se le pregunta por null.
    public ReglaDeLista<C> sinNulos(String mensaje) {
        if (valor != null && valor.stream().anyMatch(Objects::isNull)) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    // Lo que se pide de una vez: pasado el tope, el pedido es un error o un abuso, y además hace trabajar
    // al servidor por cada elemento.
    public ReglaDeLista<C> hasta(int maximo, String mensaje) {
        if (valor != null && valor.size() > maximo) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    public C valor() {
        return valor;
    }
}
