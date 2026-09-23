package ar.uade.cine.infrastructure.importador;

import java.util.List;

import ar.uade.cine.service.cartelera.DatosPelicula;

public interface CatalogoExterno {

    // Puede devolver películas inválidas: filtrarlas acá duplicaría R1 y R2.
    List<DatosPelicula> enCartelera(int paginas);

    Estado consultar();

    record Estado(boolean disponible, String detalle) {
    }
}
