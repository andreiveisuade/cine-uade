package ar.uade.cine.service.cartelera;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;

/**
 * El buzón de revisión de lo importado. Aparte de {@link GestorCartelera} porque aceptar o
 * rechazar propuestas es otro actor y otra pantalla. Depende del catálogo y no al revés.
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
     * Entra pendiente y fuera de cartelera. Método aparte y no un flag de
     * {@link GestorCartelera#agregar}: olvidarse el flag publicaría sin revisión.
     */
    public Pelicula importar(DatosPelicula datos) {
        Pelicula pelicula = catalogo.agregar(datos);
        pelicula.setEstadoRevision(EstadoRevision.PENDIENTE);
        pelicula.setEnCartelera(false);
        peliculaRepository.save(pelicula);
        return pelicula;
    }

    public List<Pelicula> listarPendientes() {
        return peliculaRepository.findByEstadoRevision(EstadoRevision.PENDIENTE);
    }

    /** Confirmada y en cartelera de una vez: «esta la damos» es el caso normal. */
    public Pelicula confirmar(int id) {
        Pelicula pelicula = exigir(id);
        pelicula.setEstadoRevision(EstadoRevision.CONFIRMADA);
        pelicula.setEnCartelera(true);
        peliculaRepository.save(pelicula);
        return pelicula;
    }

    /** Descartada en vez de borrada: si no, la próxima corrida la traería de nuevo. */
    public Pelicula descartar(int id) {
        Pelicula pelicula = exigir(id);
        if (funcionRepository.existsByPelicula_Id(id)) {
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
