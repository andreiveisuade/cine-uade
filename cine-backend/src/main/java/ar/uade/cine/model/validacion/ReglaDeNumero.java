package ar.uade.cine.model.validacion;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Guardas de un número: obligatorio, positivo, no negativo o en un rango; Fluent Interface desde Regla.
// Contra el cero compara por doubleValue(): alcanza para los enteros y los decimales que usa el modelo.
public final class ReglaDeNumero<N extends Number & Comparable<N>> {

    private final N valor;

    ReglaDeNumero(N valor) {
        this.valor = valor;
    }

    // Un número que no vino falta: no es cero.
    public ReglaDeNumero<N> obligatorio(String mensaje) {
        if (valor == null) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    public ReglaDeNumero<N> mayorQueCero(String mensaje) {
        if (valor != null && valor.doubleValue() <= 0) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    public ReglaDeNumero<N> noNegativo(String mensaje) {
        if (valor != null && valor.doubleValue() < 0) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    // Las dos puntas incluidas, como dicen los mensajes («entre 1 y 99»).
    public ReglaDeNumero<N> entre(N minimo, N maximo, String mensaje) {
        if (valor != null && (valor.compareTo(minimo) < 0 || valor.compareTo(maximo) > 0)) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    public N valor() {
        return valor;
    }
}
