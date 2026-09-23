package ar.uade.cine.service.cartelera;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.infrastructure.reloj.Reloj;

/**
 * El catálogo de películas: alta, edición, qué está en cartelera.
 *
 * <p>Recibe el gestor de grillas porque la cartelera es la lectura de la que cuelga la
 * extensión: antes de decir qué hay en cartel, las grillas activas materializan lo que
 * les toca. La dependencia va en esta dirección: las grillas no saben de la cartelera.
 */
@Service
@Transactional
public class GestorCartelera {

    private final PeliculaRepository peliculaRepository;
    private final FuncionRepository funcionRepository;
    private final GestorProgramaciones programaciones;
    private final Reloj reloj;

    public GestorCartelera(PeliculaRepository peliculaRepository, FuncionRepository funcionRepository,
                           GestorProgramaciones programaciones, Reloj reloj) {
        this.peliculaRepository = peliculaRepository;
        this.funcionRepository = funcionRepository;
        this.programaciones = programaciones;
        this.reloj = reloj;
    }

    /** El alta mínima: título, duración, géneros y clasificación. */
    public Pelicula agregar(String titulo, int duracionMinutos, List<Genero> generos,
                            Clasificacion clasificacion) {
        return agregar(DatosPelicula.deAlta(titulo, duracionMinutos, generos, clasificacion));
    }

    /** El alta completa, con los datos de catálogo que vengan. */
    public Pelicula agregar(DatosPelicula datos) {
        int duracion = datos.duracionMinutos() == null ? 0 : datos.duracionMinutos();
        validar(datos.titulo(), duracion, datos.generos(), datos.clasificacion());
        validarTituloLibre(datos.titulo(), 0);

        Pelicula pelicula = new Pelicula(datos.titulo(), duracion, datos.generos(),
                datos.clasificacion());
        aplicarCatalogo(pelicula, datos);
        peliculaRepository.save(pelicula);
        return pelicula;
    }

    /**
     * Edición parcial: lo que viene en {@code null} conserva el valor que tenía. Se valida
     * con las mismas reglas que el alta, así una edición no es una puerta de atrás para
     * dejar una película sin título o con duración cero.
     */
    public Pelicula editar(int id, DatosPelicula cambios) {
        Pelicula actual = peliculaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + id));

        String titulo = cambios.titulo() == null ? actual.getTitulo() : cambios.titulo();
        int duracion = cambios.duracionMinutos() == null
                ? actual.getDuracionMinutos() : cambios.duracionMinutos();
        List<Genero> generos = cambios.generos() == null ? actual.getGeneros() : cambios.generos();
        Clasificacion clasificacion = cambios.clasificacion() == null
                ? actual.getClasificacion() : cambios.clasificacion();
        validar(titulo, duracion, generos, clasificacion);
        validarTituloLibre(titulo, id);

        // Se edita la guardada y no una copia: lo que el pedido no trae —incluido el estado
        // de revisión— queda como estaba.
        actual.actualizar(titulo, duracion, generos, clasificacion);
        aplicarCatalogo(actual, cambios);

        peliculaRepository.save(actual);
        return actual;
    }

    /** Guarda una película ya modificada, con R1: renombrarla no puede duplicar un título. */
    public void actualizar(Pelicula pelicula) {
        if (!peliculaRepository.existsById(pelicula.getId())) {
            throw new IllegalArgumentException("No existe la película " + pelicula.getId());
        }
        validarTituloLibre(pelicula.getTitulo(), pelicula.getId());
        peliculaRepository.save(pelicula);
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
        if (peliculaRepository.existsByTituloIgnoreCaseAndIdNot(titulo, exceptoId)) {
            throw new IllegalArgumentException("Ya existe una película con ese título");
        }
    }

    /** Lo que no se está editando sigue como estaba. */
    private void aplicarCatalogo(Pelicula pelicula, DatosPelicula datos) {
        if (datos.puntaje() != null) {
            if (datos.puntaje() < 0 || datos.puntaje() > 10) {
                throw new IllegalArgumentException("El puntaje va de 0 a 10");
            }
            pelicula.setPuntaje(datos.puntaje());
        }
        if (datos.votos() != null) {
            if (datos.votos() < 0) {
                throw new IllegalArgumentException("Los votos no pueden ser negativos");
            }
            pelicula.setVotos(datos.votos());
        }
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

    /**
     * Lo que ve el cliente: las películas con alguna función por delante. Estar en
     * cartelera <strong>se deriva</strong> de las funciones, no se declara: un flag a mano
     * siempre termina mintiendo. El flag {@code enCartelera} quedó como veto del
     * administrador: puede sacar una película que tiene funciones, no meter una que no.
     */
    public List<Pelicula> listarEnCartelera() {
        LocalDateTime ahora = reloj.ahora();
        // Las grillas activas materializan acá lo que les falta: sin esto un cine con
        // grillas abiertas amanecería vacío al pasar el último rango generado.
        programaciones.extenderActivas(ahora.toLocalDate());
        Set<Integer> conFuncionesPorDelante = funcionRepository.findAll().stream()
                .filter(funcion -> !funcion.yaEmpezo(ahora))
                .map(Funcion::getPeliculaId)
                .collect(Collectors.toSet());

        return peliculaRepository.findAll().stream()
                .filter(Pelicula::estaEnCartelera)
                .filter(pelicula -> conFuncionesPorDelante.contains(pelicula.getId()))
                .toList();
    }

    public List<Pelicula> listar() {
        return peliculaRepository.findAll();
    }

    /**
     * El catálogo filtrado; cualquier parámetro en {@code null} no filtra.
     *
     * @param publicada {@code Boolean} y no {@code boolean} para que exista el tercer caso: todas
     */
    public List<Pelicula> buscar(String titulo, Genero genero, Boolean publicada) {
        String buscado = titulo == null ? "" : titulo.trim().toLowerCase();
        return peliculaRepository.findAll().stream()
                .filter(p -> buscado.isEmpty() || p.getTitulo().toLowerCase().contains(buscado))
                .filter(p -> genero == null || p.getGeneros().contains(genero))
                .filter(p -> publicada == null || p.estaEnCartelera() == publicada)
                .toList();
    }

    public Optional<Pelicula> buscar(int id) {
        return peliculaRepository.findById(id);
    }

    /**
     * R12: una película con funciones o en una grilla no se borra; para sacarla de
     * circulación está {@code enCartelera}. La grilla cuenta aparte porque una recién
     * creada puede no haber materializado nada todavía, y sin el chequeo el borrado
     * terminaba en un 500 de la foreign key. Los mensajes nombran por título, que es lo
     * que el usuario ve.
     */
    public void eliminar(int id) {
        Pelicula pelicula = peliculaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + id));
        if (funcionRepository.existsByPeliculaId(id)) {
            throw new IllegalArgumentException("La película " + pelicula.getTitulo()
                    + " tiene funciones programadas: sacala de cartelera en vez de borrarla");
        }
        if (!programaciones.buscar(id, null, null).isEmpty()) {
            throw new IllegalArgumentException("La película " + pelicula.getTitulo()
                    + " está programada en una grilla: sacala de cartelera en vez de borrarla");
        }
        peliculaRepository.deleteById(id);
    }
}
