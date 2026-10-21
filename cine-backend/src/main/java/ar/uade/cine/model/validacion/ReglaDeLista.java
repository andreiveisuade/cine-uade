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

    public ReglaDeLista<C> noVacia(String mensaje) {
        if (valor == null || valor.isEmpty()) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    // `[null]` llega del JSON como lista de un elemento; por stream porque List.of(...).contains(null) tira NPE.
    public ReglaDeLista<C> sinNulos(String mensaje) {
        if (valor != null && valor.stream().anyMatch(Objects::isNull)) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    // Pasado el tope el pedido es un error o abuso, y hace trabajar al servidor por cada elemento.
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
