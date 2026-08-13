package ar.uade.cine.servicio;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.cartelera.PeliculaImpl;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;

/**
 * Reglas de negocio del catálogo. Depende de la interfaz PeliculaDAO, no de una
 * implementación concreta: por eso funciona igual con memoria, MySQL o lo que venga.
 */
public class GestorCartelera {

    private final PeliculaDAO peliculaDAO;
    private final FuncionDAO funcionDAO;

    public GestorCartelera(PeliculaDAO peliculaDAO, FuncionDAO funcionDAO) {
        this.peliculaDAO = peliculaDAO;
        this.funcionDAO = funcionDAO;
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
        validarTituloLibre(titulo, 0);

        Pelicula pelicula = new PeliculaImpl(titulo, duracionMinutos, generos, clasificacion);
        peliculaDAO.guardar(pelicula);
        return pelicula;
    }

    /** R1 también al editar: si no, renombrar una película permitiría duplicar un título. */
    public void actualizar(Pelicula pelicula) {
        if (peliculaDAO.buscarPorId(pelicula.getId()).isEmpty()) {
            throw new IllegalArgumentException("No existe la película " + pelicula.getId());
        }
        validarTituloLibre(pelicula.getTitulo(), pelicula.getId());
        peliculaDAO.actualizar(pelicula);
    }

    /** exceptoId 0 al dar de alta: ninguna película guardada tiene ese id. */
    private void validarTituloLibre(String titulo, int exceptoId) {
        boolean repetida = peliculaDAO.listar().stream()
                .filter(p -> p.getId() != exceptoId)
                .anyMatch(p -> p.getTitulo().equalsIgnoreCase(titulo));
        if (repetida) {
            throw new IllegalArgumentException("Ya existe una película con ese título");
        }
    }

    /** Solo películas en cartelera: es lo que ve el cliente. */
    public List<Pelicula> listarEnCartelera() {
        return peliculaDAO.listar().stream().filter(Pelicula::estaEnCartelera).toList();
    }

    public List<Pelicula> listarPorGenero(Genero genero) {
        return peliculaDAO.listar().stream()
                .filter(p -> p.getGeneros().contains(genero))
                .toList();
    }

    public List<Pelicula> listar() {
        return peliculaDAO.listar();
    }

    public Optional<Pelicula> buscar(int id) {
        return peliculaDAO.buscarPorId(id);
    }

    /**
     * R12: una película con funciones no se borra. Para sacarla de circulación sin perder
     * el historial está estaEnCartelera, que es lo que corresponde casi siempre.
     */
    public void eliminar(int id) {
        if (peliculaDAO.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe la película " + id);
        }
        if (!funcionDAO.listarPorPelicula(id).isEmpty()) {
            throw new IllegalArgumentException("La película " + id
                    + " tiene funciones programadas: sacala de cartelera en vez de borrarla");
        }
        peliculaDAO.eliminar(id);
    }
}
