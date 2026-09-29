package ar.uade.cine.model.validacion;

import java.util.Collection;

// Punto de entrada de las guardas que validan un dato sin cadenas de if; Fluent Interface.
// Cada guarda recibe el mensaje de quien la usa, así cada entidad conserva sus textos, y rechaza con
// DatoInvalido. La cadena termina en valor(), que devuelve el dato ya validado:
//   String titulo = Regla.texto(t).obligatorio("Falta el título").recortado()
//           .hasta(100, "El título").valor();
//   int duracion = Regla.numero(minutos).obligatorio("Falta la duración")
//           .mayorQueCero("La duración tiene que ser mayor a cero").valor();
// Salvo obligatorio(), ninguna guarda mira un null: un dato opcional se valida solo si vino.
public final class Regla {

    private Regla() {
    }

    public static ReglaDeTexto texto(String valor) {
        return new ReglaDeTexto(valor);
    }

    // Genérica para que la duración (Integer) y el puntaje (Double) pasen por las mismas guardas.
    public static <N extends Number & Comparable<N>> ReglaDeNumero<N> numero(N valor) {
        return new ReglaDeNumero<>(valor);
    }

    // Para un mapa del pedido (butaca → tarifa, producto → cantidad) se le pasa keySet() o values().
    public static <C extends Collection<?>> ReglaDeLista<C> lista(C valor) {
        return new ReglaDeLista<>(valor);
    }

    public static <T> ReglaDeObjeto<T> objeto(T valor) {
        return new ReglaDeObjeto<>(valor);
    }
}
