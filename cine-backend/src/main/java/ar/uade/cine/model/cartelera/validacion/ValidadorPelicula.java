package ar.uade.cine.model.cartelera.validacion;

import java.util.List;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.validacion.Regla;

// Valida título, duración, géneros y clasificación de una película; guardas de Regla que llama Pelicula.
// Cada método devuelve el dato listo para guardar (recortado, sin repetidos): Pelicula llama a los
// cuatro antes de asignar nada, así una edición rechazada no deja la película a medio cambiar.
public final class ValidadorPelicula {

    // Ninguna película de cartelera dura diez horas. Sin tope, 2147483647 desbordaba el margen con el que
    // GestorFunciones busca superposiciones y apagaba R3 en todas las salas.
    private static final int DURACION_MAXIMA = 600;

    private ValidadorPelicula() {
    }

    // Recortado: " Matrix" pasaría el chequeo de título repetido de R1 como si fuera otra película.
    public static String titulo(String titulo) {
        return Regla.texto(titulo).obligatorio("Falta el título").recortado().hasta(100, "El título").valor();
    }

    // Integer porque en el alta viene del pedido: la que no vino falta, no es cero.
    public static int duracion(Integer minutos) {
        return Regla.numero(minutos).obligatorio("Falta la duración")
                .entre(1, DURACION_MAXIMA, "La duración tiene que estar entre 1 y " + DURACION_MAXIMA + " minutos")
                .valor();
    }

    // Sin repetidos: la clave de pelicula_genero es (pelicula_id, genero). Se juntan en vez de rechazarse:
    // mandar dos veces el mismo género dice lo mismo que mandarlo una.
    public static List<Genero> generos(List<Genero> generos) {
        return Regla.lista(generos).noVacia("La película tiene que tener al menos un género")
                .sinNulos("Falta el género").valor().stream().distinct().toList();
    }

    public static Clasificacion clasificacion(Clasificacion clasificacion) {
        return Regla.objeto(clasificacion).obligatorio("Falta la clasificación por edad").valor();
    }
}
