package ar.uade.cine.model.validacion;

import java.util.Collection;

// Punto de entrada de las guardas que validan un dato sin cadenas de if; Fluent Interface.
// Cada guarda recibe el mensaje de quien la usa y rechaza con DatoInvalido; la cadena termina en valor().
// Salvo obligatorio(), ninguna guarda mira un null: un dato opcional se valida solo si vino.
public final class Regla {

    private Regla() {
    }

    public static ReglaDeTexto texto(String valor) {
        return new ReglaDeTexto(valor);
    }

    public static <N extends Number & Comparable<N>> ReglaDeNumero<N> numero(N valor) {
        return new ReglaDeNumero<>(valor);
    }

    public static <C extends Collection<?>> ReglaDeLista<C> lista(C valor) {
        return new ReglaDeLista<>(valor);
    }

    public static <T> ReglaDeObjeto<T> objeto(T valor) {
        return new ReglaDeObjeto<>(valor);
    }
}
