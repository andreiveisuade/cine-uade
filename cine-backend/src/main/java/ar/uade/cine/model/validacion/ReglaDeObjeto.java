package ar.uade.cine.model.validacion;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Guarda de un dato que tiene que venir (un enum, una fecha, otra entidad); Fluent Interface desde Regla.
public final class ReglaDeObjeto<T> {

    private final T valor;

    ReglaDeObjeto(T valor) {
        this.valor = valor;
    }

    public ReglaDeObjeto<T> obligatorio(String mensaje) {
        if (valor == null) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    public T valor() {
        return valor;
    }
}
