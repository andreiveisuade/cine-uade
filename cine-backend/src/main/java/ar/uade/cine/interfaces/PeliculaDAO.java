package ar.uade.cine.interfaces;

import java.util.List;
import java.util.Optional;


/**
 * El contrato: qué operaciones existen. No dice dónde ni cómo se guardan los datos.
 * Devuelve datos, nunca imprime: quien llama decide qué hacer con el resultado.
 */
public interface PeliculaDAO {

    void guardar(Pelicula pelicula);

    /** Para editar la película: datos de catálogo, o sacarla de cartelera. */
    void actualizar(Pelicula pelicula);

    Optional<Pelicula> buscarPorId(int id);

    List<Pelicula> listar();

    void eliminar(int id);
}
