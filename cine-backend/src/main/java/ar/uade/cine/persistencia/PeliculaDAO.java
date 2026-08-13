package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.cartelera.Pelicula;

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
