package ar.uade.cine.api;

import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.servicio.CalculadoraPrecio;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorSalas;
import ar.uade.cine.servicio.Ocupacion;

/**
 * Arma las películas y las funciones en la forma que espera el front: los DTO de
 * {@link ar.uade.cine.dto}.
 *
 * <p>Una función arrastra más de lo que guarda: la sala donde va, la película que
 * proyecta, el precio de cada butaca y cuáles están tomadas. Nada de eso vive en la
 * función, así que hay que ir a buscarlo. Ese es el trabajo de esta clase, y es la razón
 * por la que el DTO no se sabe armar solo: tendría que conocer estos cuatro gestores.
 *
 * <p>Qué butaca está ocupada se le pregunta a {@link Ocupacion} y no se recalcula acá: es
 * una regla de negocio, no una cuestión de formato.
 */
public class VistasCartelera {

    private final GestorCartelera cartelera;
    private final GestorSalas salas;
    private final Ocupacion ocupacion;
    private final CalculadoraPrecio calculadora;
    private final VistasSalas vistasSalas;

    public VistasCartelera(GestorCartelera cartelera, GestorSalas salas, Ocupacion ocupacion,
                           CalculadoraPrecio calculadora, VistasSalas vistasSalas) {
        this.cartelera = cartelera;
        this.salas = salas;
        this.ocupacion = ocupacion;
        this.calculadora = calculadora;
        this.vistasSalas = vistasSalas;
    }

    public PeliculaVistaDTO pelicula(Pelicula p) {
        return new PeliculaVistaDTO(p.getId(), p.getTitulo(), p.getDuracionMinutos(),
                p.getGeneros().stream().map(Enum::name).toList(),
                p.getClasificacion().name(), p.getPosterUrl(), p.getDirector(), p.getAnio(),
                p.getIdiomaOriginal(), p.getSinopsis(), p.estaEnCartelera(),
                p.getEstadoRevision().name());
    }

    /** Para el listado del cliente: la función con su sala, sin el mapa de butacas. */
    public FuncionVistaDTO funcion(Funcion f) {
        return armar(f, null, null, null);
    }

    /** Para el listado del encargado, que muestra qué película va en cada función. */
    public FuncionVistaDTO funcionConPelicula(Funcion f) {
        return armar(f, peliculaDe(f), null, null);
    }

    /** El mapa de butacas: cada asiento de la sala con su precio y si está tomado acá. */
    public FuncionVistaDTO funcionConButacas(Funcion f) {
        Sala sala = salaDe(f);
        Set<Integer> ocupados = ocupacion.asientosOcupados(f.getId());
        List<AsientoVistaDTO> butacas = salas.asientosDe(sala.getId()).stream()
                .map(a -> vistasSalas.asiento(a, f, sala, ocupados))
                .toList();
        return armar(f, peliculaDe(f), butacas, ocupacion.lugaresLibres(f.getId()));
    }

    private FuncionVistaDTO armar(Funcion f, PeliculaVistaDTO pelicula, List<AsientoVistaDTO> butacas,
                               Integer libres) {
        Sala sala = salaDe(f);
        List<Asiento> asientos = salas.asientosDe(sala.getId());
        return new FuncionVistaDTO(f.getId(), f.getPeliculaId(), f.getSalaId(),
                Fechas.texto(f.getInicio()), f.getVersion().name(), f.getProyeccion().name(),
                f.getPrecio(), calculadora.precioBaseEnSala(f, sala),
                vistasSalas.sala(sala, asientos), pelicula, butacas, libres);
    }

    private Sala salaDe(Funcion f) {
        return salas.buscar(f.getSalaId())
                .orElseThrow(() -> new NoEncontrado("No existe la sala " + f.getSalaId()));
    }

    private PeliculaVistaDTO peliculaDe(Funcion f) {
        return cartelera.buscar(f.getPeliculaId()).map(this::pelicula).orElse(null);
    }
}
