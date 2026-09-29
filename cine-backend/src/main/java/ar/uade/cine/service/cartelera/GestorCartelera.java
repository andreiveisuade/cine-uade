package ar.uade.cine.service.cartelera;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Catálogo de películas (R1, R12); coordina: la película valida sus datos, el gestor lo que pide la base.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorCartelera {

    private final PeliculaRepository peliculaRepository;
    private final FuncionRepository funcionRepository;
    private final GestorProgramaciones programaciones;
    private final Reloj reloj;

    public Pelicula agregar(String titulo, int duracionMinutos, List<Genero> generos,
                            Clasificacion clasificacion) {
        return agregar(DatosPelicula.deAlta(titulo, duracionMinutos, generos, clasificacion));
    }

    // Los datos los valida la película; acá queda el título repetido, que necesita la base.
    // Construirla primero rechaza un dato inválido antes que un título repetido.
    public Pelicula agregar(DatosPelicula datos) {
        Pelicula pelicula = new Pelicula(datos.titulo(), datos.duracionMinutos(), datos.generos(),
                datos.clasificacion());
        exigirTituloLibre(pelicula);
        aplicarCatalogo(pelicula, datos);
        peliculaRepository.save(pelicula);
        return pelicula;
    }

    public Pelicula editar(int id, DatosPelicula cambios) {
        Pelicula actual = exigir(id);

        String titulo = cambios.titulo() == null ? actual.getTitulo() : cambios.titulo();
        int duracion = cambios.duracionMinutos() == null
                ? actual.getDuracionMinutos() : cambios.duracionMinutos();
        List<Genero> generos = cambios.generos() == null ? actual.getGeneros() : cambios.generos();
        Clasificacion clasificacion = cambios.clasificacion() == null
                ? actual.getClasificacion() : cambios.clasificacion();
        actual.actualizar(titulo, duracion, generos, clasificacion);
        // Después de actualizar, por el mismo orden que el alta. La consulta hace flush de la
        // fila ya cambiada, pero se excluye a sí misma y el rechazo deshace la transacción.
        exigirTituloLibre(actual);
        aplicarCatalogo(actual, cambios);
        return actual;
    }

    // El título de la película y no el del pedido: ya viene recortado, así los espacios no burlan R1.
    // Una sin guardar tiene id 0, que ninguna guardada tiene: en el alta no excluye a nadie.
    private void exigirTituloLibre(Pelicula pelicula) {
        if (peliculaRepository.existsByTituloIgnoreCaseAndIdNot(pelicula.getTitulo(), pelicula.getId())) {
            throw new ConflictoDeNegocio("Ya existe una película con ese título");
        }
    }

    // Solo lo que vino en el pedido: en una edición, null es "no lo mandé" y el catálogo deja lo que había.
    private void aplicarCatalogo(Pelicula pelicula, DatosPelicula datos) {
        pelicula.cambiarCatalogo(pelicula.getCatalogo().conCambios(datos.director(), datos.sinopsis(),
                datos.anio(), datos.idiomaOriginal(), datos.posterUrl(), datos.puntaje(), datos.votos(),
                reloj.hoy()));
        if (Boolean.TRUE.equals(datos.enCartelera())) {
            pelicula.ponerEnCartelera();
        } else if (Boolean.FALSE.equals(datos.enCartelera())) {
            pelicula.sacarDeCartelera();
        }
    }

    // En cartelera = tiene funciones por delante; el flag enCartelera es solo un veto.
    // Sin transacción propia: si no, retendría su conexión mientras extenderActivas, que corre fuera
    // de transacción, pide otra. Con el pool entero pidiendo la cartelera a la vez, todos esperarían.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Pelicula> listarEnCartelera(Genero genero) {
        LocalDateTime ahora = reloj.ahora();
        // Sin esto, un cine con grillas abiertas amanecería vacío al pasar el último rango.
        programaciones.extenderActivas(ahora.toLocalDate());
        return peliculaRepository.findEnCartelera(ahora, genero);
    }

    @Transactional(readOnly = true)
    public List<Pelicula> listar() {
        return peliculaRepository.findAll();
    }

    // Todos, descartadas incluidas: es contra lo que el importador compara lo que trae.
    @Transactional(readOnly = true)
    public List<String> titulos() {
        return peliculaRepository.titulos();
    }

    @Transactional(readOnly = true)
    public List<Pelicula> buscar(String titulo, Genero genero, Boolean publicada) {
        return peliculaRepository.buscar(titulo == null ? "" : titulo.trim().toLowerCase(),
                genero, publicada);
    }

    @Transactional(readOnly = true)
    public List<Pelicula> buscar(Collection<Integer> ids) {
        return peliculaRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public Optional<Pelicula> buscar(int id) {
        return peliculaRepository.findById(id);
    }

    // La grilla se chequea aparte: puede no haber generado funciones y el borrado daría 500 por la FK.
    public void eliminar(int id) {
        Pelicula pelicula = exigir(id);
        if (funcionRepository.existsByPelicula_Id(id)) {
            throw new DatoInvalido("La película " + pelicula.getTitulo()
                    + " tiene funciones programadas: sacala de cartelera en vez de borrarla");
        }
        if (!programaciones.buscar(id, null, null).isEmpty()) {
            throw new DatoInvalido("La película " + pelicula.getTitulo()
                    + " está programada en una grilla: sacala de cartelera en vez de borrarla");
        }
        peliculaRepository.deleteById(id);
    }

    private Pelicula exigir(int id) {
        return peliculaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la película " + id));
    }
}
