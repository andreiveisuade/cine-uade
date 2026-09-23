package ar.uade.cine.service.cartelera;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;

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

    public Pelicula confirmar(int id) {
        Pelicula pelicula = exigir(id);
        pelicula.setEstadoRevision(EstadoRevision.CONFIRMADA);
        pelicula.setEnCartelera(true);
        peliculaRepository.save(pelicula);
        return pelicula;
    }

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
