package ar.uade.cine.servicio.cartelera;

import java.util.List;

import ar.uade.cine.dominio.cartelera.EstadoRevision;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;

/**
 * El buzón de revisión: lo que trae el importador y todavía nadie miró.
 *
 * <h2>Por qué está separado del catálogo</h2>
 * <p>{@link GestorCartelera} administra el catálogo —el alta, la edición, qué está en
 * cartelera— y eso es lo que hace el encargado cuando carga una película a mano. Esto es
 * otro circuito: son <em>propuestas</em> que llegaron solas desde TMDB y que alguien tiene
 * que aceptar o rechazar. Tienen distinto actor, distinta pantalla y distinta razón para
 * cambiar: el catálogo cambia cuando cambia qué datos tiene una película, y el buzón
 * cuando cambia cómo se decide qué entra.
 *
 * <p>Vivían juntos y se notaba: {@code GestorCartelera} necesitaba la palabra "y" para
 * describirse —"administra el catálogo <em>y</em> revisa lo que trae el importador"— que es
 * la señal de que hay dos responsabilidades adentro de una clase.
 *
 * <p>Depende de {@link GestorCartelera} y no al revés: revisar necesita dar de alta, pero
 * el catálogo no necesita saber que existe un importador. Es la misma dirección que tiene
 * la dependencia entre cartelera y programaciones.
 */
public class GestorRevisionCartelera {

    private final PeliculaDAO peliculaDAO;
    private final FuncionDAO funcionDAO;
    private final GestorCartelera catalogo;

    public GestorRevisionCartelera(PeliculaDAO peliculaDAO, FuncionDAO funcionDAO,
                                   GestorCartelera catalogo) {
        this.peliculaDAO = peliculaDAO;
        this.funcionDAO = funcionDAO;
        this.catalogo = catalogo;
    }

    /**
     * El alta del importador: la película entra al buzón, no al catálogo.
     *
     * <p>Es un método aparte y no un parámetro de {@link GestorCartelera#agregar} porque
     * son dos circuitos distintos y conviene que se note: lo que carga el encargado ya está
     * decidido por definición —lo decidió al cargarlo—, y lo que baja de TMDB es una
     * propuesta. Si fueran el mismo método con un flag, olvidarse el flag haría entrar
     * dieciocho títulos al catálogo sin que nadie los mire, que es exactamente lo que esto
     * viene a evitar.
     *
     * <p>Nace fuera de cartelera además de pendiente: una película que nadie confirmó no
     * puede estar ofreciéndose al cliente mientras espera que la miren.
     */
    public Pelicula importar(DatosPelicula datos) {
        Pelicula pelicula = catalogo.agregar(datos);
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
}
