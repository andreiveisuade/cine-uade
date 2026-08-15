package ar.uade.cine.infraestructura.importador;

import java.util.List;

import ar.uade.cine.servicio.cartelera.DatosPelicula;

/**
 * De dónde salen las películas que el cine no cargó a mano.
 *
 * <p>Es una interfaz por el mismo motivo que {@link ar.uade.cine.infraestructura.pasarelas.PasarelaPagos}:
 * lo que hay del otro lado es un tercero —hoy TMDB— y el gestor no tiene por qué saber cómo
 * se le habla. Acá adentro no aparece la palabra TMDB, ni una clave de API, ni una URL: el
 * backend pide «traeme lo que está en cartelera» y el que sabe de dónde sacarlo es el
 * adaptador.
 *
 * <p><strong>Sólo trae candidatas: no da de alta ninguna.</strong> Devuelve
 * {@link DatosPelicula}, que es la misma forma que usa el alta del encargado, y quien decide
 * qué hacer con eso es {@link ar.uade.cine.servicio.cartelera.GestorImportaciones}. La
 * separación importa: si el adaptador escribiera, las reglas de R1 y R2 quedarían de su lado
 * y habría dos lugares donde se decide qué película es válida.
 *
 * <p>Devolver {@link DatosPelicula} —y no la forma de TMDB— es lo que mantiene al resto del
 * sistema ignorante del catálogo ajeno: de acá para adentro nadie ve un {@code genre_id} ni
 * un {@code iso_3166_1}. Es la misma idea del patrón DAO, con el modelo ajeno de un lado, el
 * nuestro del otro y una capa fina en el medio que sabe convertir.
 *
 * <p>Antes de esto el que traducía era un proceso Python aparte que escribía por HTTP. Se lo
 * absorbió porque esa traducción es una decisión nuestra —qué género ajeno cae en cuál de los
 * nuestros, qué clasificación poner cuando el INCAA no publicó ninguna— y vivía fuera del
 * sistema, en otro lenguaje, acoplada por texto a los enums de {@code dominio}: renombrar una
 * constante compilaba igual y rompía recién en la corrida siguiente.
 */
public interface CatalogoExterno {

    /**
     * Lo que hoy está en cartelera, ya traducido a nuestro vocabulario.
     *
     * <p>Puede devolver películas que el cine va a rechazar —sin título, con duración cero—:
     * quien valida es el gestor, con las mismas reglas que usa para el alta a mano. Filtrarlas
     * acá sería escribir R1 y R2 por segunda vez.
     *
     * @param paginas páginas del catálogo externo a traer, de veinte títulos cada una
     * @throws ImportadorError si no se pudo llegar, si tardó demasiado, o si falta el token
     */
    List<DatosPelicula> enCartelera(int paginas);

    /**
     * Si el catálogo está en condiciones de contestar.
     *
     * <p>No tira: que no esté disponible es una respuesta, no un error. La pantalla la usa
     * para avisar antes de que alguien apriete el botón y espere en vano.
     */
    Estado consultar();

    /** @param detalle por qué no está disponible, o que está listo si sí. Se muestra tal cual */
    record Estado(boolean disponible, String detalle) {
    }
}
