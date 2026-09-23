package ar.uade.cine.infrastructure.importador;

import java.util.List;

import ar.uade.cine.service.cartelera.DatosPelicula;

/**
 * De dónde salen las películas que el cine no cargó a mano (hoy TMDB). Solo trae
 * candidatas, ya traducidas a {@link DatosPelicula}: el alta y las reglas R1/R2 son de
 * {@link ar.uade.cine.service.cartelera.GestorImportaciones}.
 */
public interface CatalogoExterno {

    /**
     * Puede devolver películas inválidas: filtrarlas acá duplicaría R1 y R2.
     *
     * @param paginas páginas del catálogo externo a traer, de veinte títulos cada una
     * @throws ImportadorError si no se pudo llegar, si tardó demasiado, o si falta el token
     */
    List<DatosPelicula> enCartelera(int paginas);

    /** No tira: no estar disponible es una respuesta que la pantalla muestra antes de importar. */
    Estado consultar();

    /** @param detalle por qué no está disponible, o que está listo si sí. Se muestra tal cual */
    record Estado(boolean disponible, String detalle) {
    }
}
