package ar.uade.cine.servicio;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Pelicula;
import ar.uade.cine.interfaces.PeliculaDAO;
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

    public void agregar(String titulo, int duracionMinutos, List<Genero> generos) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        if (duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a cero");
        }
        if (generos == null || generos.isEmpty()) {
            throw new IllegalArgumentException("La película necesita al menos un género");
        }
        boolean repetida = dao.listar().stream()
                .anyMatch(p -> p.getTitulo().equalsIgnoreCase(titulo));
        if (repetida) {
            throw new IllegalArgumentException("Ya existe una película con ese título");
        }
        dao.guardar(new PeliculaImpl(titulo, duracionMinutos, generos));
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
