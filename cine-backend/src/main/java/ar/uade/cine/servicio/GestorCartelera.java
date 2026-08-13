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

    /** El alta mínima: título, duración, géneros y clasificación. */
    public Pelicula agregar(String titulo, int duracionMinutos, List<Genero> generos,
                            Clasificacion clasificacion) {
        return agregar(DatosPelicula.deAlta(titulo, duracionMinutos, generos, clasificacion));
    }

    /**
     * El alta completa, con los datos de catálogo incluidos y en un solo guardado. Los
     * datos de catálogo que no vengan quedan como los deja la película recién creada.
     */
    public Pelicula agregar(DatosPelicula datos) {
        int duracion = datos.duracionMinutos() == null ? 0 : datos.duracionMinutos();
        validar(datos.titulo(), duracion, datos.generos(), datos.clasificacion());
        validarTituloLibre(datos.titulo(), 0);

        Pelicula pelicula = new PeliculaImpl(datos.titulo(), duracion, datos.generos(),
                datos.clasificacion());
        aplicarCatalogo(pelicula, datos);
        peliculaDAO.guardar(pelicula);
        return pelicula;
    }

    /**
     * Edición parcial: lo que viene en {@code null} conserva el valor que ya tenía.
     *
     * <p>Que el criterio viva acá y no en cada interfaz es lo que hace que editar por la
     * web y editar por consola signifiquen lo mismo. Se valida con las mismas reglas que
     * el alta: una edición no es una puerta de atrás para dejar una película sin título
     * o con duración cero.
     *
     * @param cambios solo los campos a pisar; el resto se toma de la película guardada
     */
    public Pelicula editar(int id, DatosPelicula cambios) {
        Pelicula actual = peliculaDAO.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + id));

        String titulo = cambios.titulo() == null ? actual.getTitulo() : cambios.titulo();
        int duracion = cambios.duracionMinutos() == null
                ? actual.getDuracionMinutos() : cambios.duracionMinutos();
        List<Genero> generos = cambios.generos() == null ? actual.getGeneros() : cambios.generos();
        Clasificacion clasificacion = cambios.clasificacion() == null
                ? actual.getClasificacion() : cambios.clasificacion();
        validar(titulo, duracion, generos, clasificacion);
        validarTituloLibre(titulo, id);

        // Pelicula no deja cambiar el título ni la duración de una ya creada, así que la
        // edición arma otra con el mismo id.
        Pelicula editada = new PeliculaImpl(id, titulo, duracion, clasificacion);
        generos.forEach(editada::agregarGenero);
        copiarCatalogo(actual, editada);
        aplicarCatalogo(editada, cambios);

        peliculaDAO.actualizar(editada);
        return editada;
    }

    /** R1 también al editar: si no, renombrar una película permitiría duplicar un título. */
    public void actualizar(Pelicula pelicula) {
        if (peliculaDAO.buscarPorId(pelicula.getId()).isEmpty()) {
            throw new IllegalArgumentException("No existe la película " + pelicula.getId());
        }
        validarTituloLibre(pelicula.getTitulo(), pelicula.getId());
        peliculaDAO.actualizar(pelicula);
    }

    private void validar(String titulo, int duracionMinutos, List<Genero> generos,
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

    /** Lo que no se está editando sigue como estaba. */
    private void copiarCatalogo(Pelicula desde, Pelicula hacia) {
        hacia.setDirector(desde.getDirector());
        hacia.setSinopsis(desde.getSinopsis());
        hacia.setAnio(desde.getAnio());
        hacia.setIdiomaOriginal(desde.getIdiomaOriginal());
        hacia.setPosterUrl(desde.getPosterUrl());
        hacia.setEnCartelera(desde.estaEnCartelera());
    }

    private void aplicarCatalogo(Pelicula pelicula, DatosPelicula datos) {
        if (datos.director() != null) {
            pelicula.setDirector(datos.director());
        }
        if (datos.sinopsis() != null) {
            pelicula.setSinopsis(datos.sinopsis());
        }
        if (datos.anio() != null) {
            pelicula.setAnio(datos.anio());
        }
        if (datos.idiomaOriginal() != null) {
            pelicula.setIdiomaOriginal(datos.idiomaOriginal());
        }
        if (datos.posterUrl() != null) {
            pelicula.setPosterUrl(datos.posterUrl());
        }
        if (datos.enCartelera() != null) {
            pelicula.setEnCartelera(datos.enCartelera());
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
