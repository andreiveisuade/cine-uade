package ar.uade.cine.servicio;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Pelicula;
import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.modelo.Clasificacion;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.modelo.PeliculaImpl;

/**
 * Reglas de negocio. Depende de la interfaz PeliculaDAO, no de una implementación
 * concreta: por eso funciona igual con memoria, MySQL o lo que venga.
 */
public class GestorCartelera {

    private final PeliculaDAO dao;

    public GestorCartelera(PeliculaDAO dao) {
        this.dao = dao;
    }

    public Pelicula agregar(String titulo, int duracionMinutos, List<Genero> generos,
                            Clasificacion clasificacion) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        if (duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a cero");
        }
        if (generos == null || generos.isEmpty()) {
            throw new IllegalArgumentException("La película necesita al menos un género");
        }
        if (clasificacion == null) {
            throw new IllegalArgumentException("Falta la clasificación por edad");
        }
        boolean repetida = dao.listar().stream()
                .anyMatch(p -> p.getTitulo().equalsIgnoreCase(titulo));
        if (repetida) {
            throw new IllegalArgumentException("Ya existe una película con ese título");
        }
        Pelicula pelicula = new PeliculaImpl(titulo, duracionMinutos, generos, clasificacion);
        dao.guardar(pelicula);
        return pelicula;
    }

    public void actualizar(Pelicula pelicula) {
        if (dao.buscarPorId(pelicula.getId()).isEmpty()) {
            throw new IllegalArgumentException("No existe la película " + pelicula.getId());
        }
        dao.actualizar(pelicula);
    }

    /** Solo películas en cartelera: es lo que ve el cliente. */
    public List<Pelicula> listarEnCartelera() {
        return dao.listar().stream().filter(Pelicula::estaEnCartelera).toList();
    }

    public List<Pelicula> listarPorGenero(Genero genero) {
        return dao.listar().stream()
                .filter(p -> p.getGeneros().contains(genero))
                .toList();
    }

    public List<Pelicula> listar() {
        return dao.listar();
    }

    public Optional<Pelicula> buscar(int id) {
        return dao.buscarPorId(id);
    }

    public void eliminar(int id) {
        if (dao.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe la película " + id);
        }
        dao.eliminar(id);
    }
}
