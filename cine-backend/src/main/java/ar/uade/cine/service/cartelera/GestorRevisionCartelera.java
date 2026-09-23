package ar.uade.cine.service.cartelera;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;

/**
 * El buzón de revisión: lo que trae el importador y todavía nadie miró.
 *
 * <p>Separado de {@link GestorCartelera} porque son propuestas que alguien tiene que
 * aceptar o rechazar: distinto actor, distinta pantalla y distinta razón para cambiar.
 * Juntos, el catálogo "administraba <em>y</em> revisaba", la señal de dos
 * responsabilidades en una clase. Depende del catálogo y no al revés: revisar necesita
 * dar de alta, el catálogo no necesita saber que existe un importador.
 */
@Service
@Transactional
public class GestorRevisionCartelera {

    private final PeliculaRepository peliculaRepository;
    private final FuncionRepository funcionRepository;
    private final GestorCartelera catalogo;

    public GestorRevisionCartelera(PeliculaRepository peliculaRepository, FuncionRepository funcionRepository,
                                   GestorCartelera catalogo) {
        this.peliculaRepository = peliculaRepository;
        this.funcionRepository = funcionRepository;
        this.catalogo = catalogo;
    }

    /**
     * El alta del importador: entra al buzón, pendiente y fuera de cartelera. Es un método
     * aparte y no un flag de {@link GestorCartelera#agregar}: olvidarse el flag metería
     * dieciocho títulos al catálogo sin que nadie los mire.
     */
    public Pelicula importar(DatosPelicula datos) {
        Pelicula pelicula = catalogo.agregar(datos);
        pelicula.setEstadoRevision(EstadoRevision.PENDIENTE);
        pelicula.setEnCartelera(false);
        peliculaRepository.save(pelicula);
        return pelicula;
    }

    /** Las que trajo el importador y todavía nadie miró. Es la pantalla de revisión. */
    public List<Pelicula> listarPendientes() {
        return peliculaRepository.findByEstadoRevision(EstadoRevision.PENDIENTE);
    }

    /**
     * El encargado la acepta: confirmada y en cartelera de una vez, porque «esta la damos»
     * es el caso normal. Aceptarla para más adelante ya tiene el interruptor de cartelera.
     */
    public Pelicula confirmar(int id) {
        Pelicula pelicula = exigir(id);
        pelicula.setEstadoRevision(EstadoRevision.CONFIRMADA);
        pelicula.setEnCartelera(true);
        peliculaRepository.save(pelicula);
        return pelicula;
    }

    /**
     * El encargado no la quiere. Queda descartada en vez de borrarse: si no, la próxima
     * corrida la traería de nuevo.
     */
    public Pelicula descartar(int id) {
        Pelicula pelicula = exigir(id);
        if (funcionRepository.existsByPeliculaId(id)) {
            throw new IllegalArgumentException(
                    "No se puede descartar una película que ya tiene funciones programadas");
        }
        pelicula.setEstadoRevision(EstadoRevision.DESCARTADA);
        pelicula.setEnCartelera(false);
        peliculaRepository.save(pelicula);
        return pelicula;
    }

    private Pelicula exigir(int id) {
        return peliculaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + id));
    }
}
