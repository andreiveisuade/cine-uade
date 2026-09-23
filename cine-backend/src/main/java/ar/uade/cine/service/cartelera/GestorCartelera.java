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
import ar.uade.cine.service.RecursoNoEncontrado;
import ar.uade.cine.service.ConflictoDeNegocio;

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

    public Pelicula agregar(String titulo, int duracionMinutos, List<Genero> generos,
                            Clasificacion clasificacion) {
        return agregar(DatosPelicula.deAlta(titulo, duracionMinutos, generos, clasificacion));
    }

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

    public Pelicula editar(int id, DatosPelicula cambios) {
        Pelicula actual = peliculaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la película " + id));

        String titulo = cambios.titulo() == null ? actual.getTitulo() : cambios.titulo();
        int duracion = cambios.duracionMinutos() == null
                ? actual.getDuracionMinutos() : cambios.duracionMinutos();
        List<Genero> generos = cambios.generos() == null ? actual.getGeneros() : cambios.generos();
        Clasificacion clasificacion = cambios.clasificacion() == null
                ? actual.getClasificacion() : cambios.clasificacion();
        validar(titulo, duracion, generos, clasificacion);
        validarTituloLibre(titulo, id);

        actual.actualizar(titulo, duracion, generos, clasificacion);
        aplicarCatalogo(actual, cambios);

        peliculaRepository.save(actual);
        return actual;
    }

    public void actualizar(Pelicula pelicula) {
        if (!peliculaRepository.existsById(pelicula.getId())) {
            throw new RecursoNoEncontrado("No existe la película " + pelicula.getId());
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

    // exceptoId 0 al dar de alta: ninguna película guardada tiene ese id.
    private void validarTituloLibre(String titulo, int exceptoId) {
        if (peliculaRepository.existsByTituloIgnoreCaseAndIdNot(titulo, exceptoId)) {
            throw new ConflictoDeNegocio("Ya existe una película con ese título");
        }
    }

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

    // En cartelera = tiene funciones por delante; el flag enCartelera es solo un veto.
    public List<Pelicula> listarEnCartelera() {
        LocalDateTime ahora = reloj.ahora();
        // Sin esto, un cine con grillas abiertas amanecería vacío al pasar el último rango.
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

    public List<Pelicula> listarEnCartelera(Genero genero) {
        return listarEnCartelera().stream()
                .filter(p -> genero == null || p.getGeneros().contains(genero))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Pelicula> listar() {
        return peliculaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Pelicula> buscar(String titulo, Genero genero, Boolean publicada) {
        String buscado = titulo == null ? "" : titulo.trim().toLowerCase();
        return peliculaRepository.findAll().stream()
                .filter(p -> buscado.isEmpty() || p.getTitulo().toLowerCase().contains(buscado))
                .filter(p -> genero == null || p.getGeneros().contains(genero))
                .filter(p -> publicada == null || p.estaEnCartelera() == publicada)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Pelicula> buscar(int id) {
        return peliculaRepository.findById(id);
    }

    // La grilla se chequea aparte: puede no haber generado funciones y el borrado daría 500 por la FK.
    public void eliminar(int id) {
        Pelicula pelicula = peliculaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la película " + id));
        if (funcionRepository.existsByPelicula_Id(id)) {
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
