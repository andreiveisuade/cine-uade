package ar.uade.cine.service.cartelera;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.service.RecursoNoEncontrado;

// Buzón de lo que trae el importador: confirmar o descartar; el alta pasa por GestorCartelera y sus reglas.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorRevisionCartelera {

    private final PeliculaRepository peliculaRepository;
    private final FuncionRepository funcionRepository;
    private final GestorCartelera catalogo;

    // El alta corre en esta misma transacción y la deja gestionada: dejarla pendiente no pide otro save.
    public Pelicula importar(DatosPelicula datos) {
        Pelicula pelicula = catalogo.agregar(datos);
        pelicula.dejarPendiente();
        return pelicula;
    }

    @Transactional(readOnly = true)
    public List<Pelicula> listarPendientes() {
        return peliculaRepository.findByEstadoRevision(EstadoRevision.PENDIENTE);
    }

    public Pelicula confirmar(int id) {
        Pelicula pelicula = exigir(id);
        pelicula.confirmar();
        return pelicula;
    }

    public Pelicula descartar(int id) {
        Pelicula pelicula = exigir(id);
        if (funcionRepository.existsByPelicula_Id(id)) {
            throw new IllegalArgumentException(
                    "No se puede descartar una película que ya tiene funciones programadas");
        }
        pelicula.descartar();
        return pelicula;
    }

    private Pelicula exigir(int id) {
        return peliculaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la película " + id));
    }
}
