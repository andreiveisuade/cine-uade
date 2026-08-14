package ar.uade.cine.servicio;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.EstadoRevision;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.cartelera.PeliculaImpl;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;

/**
 * Reglas de negocio del catálogo. Depende de la interfaz PeliculaDAO, no de una
 * implementación concreta: por eso funciona igual con memoria, MySQL o lo que venga.
 */
public class GestorCartelera {

    private final PeliculaDAO peliculaDAO;
    private final FuncionDAO funcionDAO;
    private final GestorProgramaciones programaciones;

    /**
     * Recibe el gestor de grillas porque la cartelera es la lectura de la que cuelga la
     * extensión: antes de decir qué hay en cartel hay que asegurarse de que las grillas
     * activas hayan materializado lo que les toca. Es la misma dependencia entre gestores
     * que ya tienen {@code GestorPagos} con {@code GestorPromociones}, y va en esta
     * dirección y no en la otra: las grillas no necesitan saber nada de la cartelera.
     */
    public GestorCartelera(PeliculaDAO peliculaDAO, FuncionDAO funcionDAO,
                           GestorProgramaciones programaciones) {
        this.peliculaDAO = peliculaDAO;
        this.funcionDAO = funcionDAO;
        this.programaciones = programaciones;
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
     * El alta del importador: la película entra al buzón, no al catálogo.
     *
     * <p>Es un método aparte y no un parámetro de {@link #agregar(DatosPelicula)} porque
     * son dos circuitos distintos y conviene que se note: lo que carga el encargado ya
     * está decidido por definición —lo decidió al cargarlo—, y lo que baja de TMDB es una
     * propuesta. Si fueran el mismo método con un flag, olvidarse el flag haría entrar
     * dieciocho títulos al catálogo sin que nadie los mire, que es exactamente lo que esto
     * viene a evitar.
     *
     * <p>Nace fuera de cartelera además de pendiente: una película que nadie confirmó no
     * puede estar ofreciéndose al cliente mientras espera que la miren.
     */
    public Pelicula importar(DatosPelicula datos) {
        Pelicula pelicula = agregar(datos);
        pelicula.setEstadoRevision(EstadoRevision.PENDIENTE);
        pelicula.setEnCartelera(false);
        peliculaDAO.actualizar(pelicula);
        return pelicula;
    }

    /** Las que trajo el importador y todavía nadie miró. Es la pantalla de revisión. */
    public List<Pelicula> listarPendientes() {
        return peliculaDAO.listar().stream()
                .filter(p -> p.getEstadoRevision() == EstadoRevision.PENDIENTE)
                .toList();
    }

    /**
     * El encargado la acepta: pasa a ser parte del catálogo y queda en cartelera.
     *
     * <p>Hace las dos cosas a propósito. Confirmar una película importada es decir «esta
     * la damos»; dejarla confirmada pero fuera de cartelera obligaría a dos pasos para el
     * caso normal. El caso raro —aceptarla ahora para darla más adelante— ya tiene su
     * herramienta: el mismo interruptor de cartelera que tienen todas.
     */
    public Pelicula confirmar(int id) {
        Pelicula pelicula = exigir(id);
        pelicula.setEstadoRevision(EstadoRevision.CONFIRMADA);
        pelicula.setEnCartelera(true);
        peliculaDAO.actualizar(pelicula);
        return pelicula;
    }

    /**
     * El encargado no la quiere. Queda guardada como descartada en vez de borrarse: si se
     * borrara, la próxima corrida del importador la traería igual y habría que descartarla
     * de nuevo, y así para siempre.
     */
    public Pelicula descartar(int id) {
        Pelicula pelicula = exigir(id);
        if (!funcionDAO.listarPorPelicula(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "No se puede descartar una película que ya tiene funciones programadas");
        }
        pelicula.setEstadoRevision(EstadoRevision.DESCARTADA);
        pelicula.setEnCartelera(false);
        peliculaDAO.actualizar(pelicula);
        return pelicula;
    }

    private Pelicula exigir(int id) {
        return peliculaDAO.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + id));
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

    /**
     * Lo que ve el cliente: las películas que tienen alguna función por delante.
     *
     * <p>Estar en cartelera <strong>se deriva</strong>, no se declara. Antes era un flag
     * que alguien tenía que mantener a mano, y un flag así siempre termina mintiendo: se
     * programan funciones y nadie lo prende, o la última función pasó hace un mes y sigue
     * anunciada. Es el mismo argumento por el que la sala no guarda su capacidad — dos
     * fuentes de verdad para el mismo hecho se separan solas.
     *
     * <p>El flag no desapareció, cambió de rol: pasó de <em>anuncio</em> a <em>veto</em>.
     * Puede sacar de la cartelera una película que tiene funciones, pero no puede meter
     * una que no las tiene. Eso lo deja como interruptor del administrador —para bajar
     * algo de la web sin tocar la programación— sin volver a ser una fuente de verdad
     * paralela: cuando los dos hablan, manda la programación.
     */
    public List<Pelicula> listarEnCartelera() {
        LocalDateTime ahora = LocalDateTime.now();
        // Las grillas activas materializan acá lo que les falta. Sin esto un cine con
        // grillas abiertas amanecería vacío el día que se pasara el último rango generado:
        // la cartelera se deriva de las funciones, y las funciones alguien las tiene que
        // crear. Es el mismo momento que elige GestorReservas para expirar las vencidas —
        // el trabajo lo hace quien consulta, no un proceso de fondo.
        programaciones.extenderActivas(ahora.toLocalDate());
        Set<Integer> conFuncionesPorDelante = funcionDAO.listar().stream()
                .filter(funcion -> !funcion.yaEmpezo(ahora))
                .map(Funcion::getPeliculaId)
                .collect(Collectors.toSet());

        return peliculaDAO.listar().stream()
                .filter(Pelicula::estaEnCartelera)
                .filter(pelicula -> conFuncionesPorDelante.contains(pelicula.getId()))
                .toList();
    }

    public List<Pelicula> listarPorGenero(Genero genero) {
        return peliculaDAO.listar().stream()
                .filter(p -> p.getGeneros().contains(genero))
                .toList();
    }

    public List<Pelicula> listar() {
        return peliculaDAO.listar();
    }

    /**
     * El catálogo filtrado. Cualquier parámetro en {@code null} no filtra.
     *
     * <p>Desde que el importador trae la cartelera real, el catálogo dejó de ser una lista
     * de cuatro títulos de prueba: son veintidós y van a ser más en cada corrida. Buscar
     * por título es lo primero que se necesita cuando la lista pasa de una pantalla.
     *
     * @param publicada {@code true} solo las publicadas, {@code false} solo las
     *                  despublicadas, {@code null} todas. Es {@code Boolean} y no
     *                  {@code boolean} justamente para que exista ese tercer caso
     */
    public List<Pelicula> buscar(String titulo, Genero genero, Boolean publicada) {
        String buscado = titulo == null ? "" : titulo.trim().toLowerCase();
        return peliculaDAO.listar().stream()
                .filter(p -> buscado.isEmpty() || p.getTitulo().toLowerCase().contains(buscado))
                .filter(p -> genero == null || p.getGeneros().contains(genero))
                .filter(p -> publicada == null || p.estaEnCartelera() == publicada)
                .toList();
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
