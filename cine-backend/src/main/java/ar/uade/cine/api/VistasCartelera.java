package ar.uade.cine.api;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.api.VistasSalas.AsientoVista;
import ar.uade.cine.api.VistasSalas.SalaVista;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.servicio.CalculadoraPrecio;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;

/**
 * Las películas y las funciones, en la forma que espera el front.
 *
 * <p>Una función arrastra más de lo que guarda: la sala donde va, la película que
 * proyecta, el precio de cada butaca y cuáles están tomadas. Nada de eso vive en la
 * función, así que hay que ir a buscarlo. Qué butaca está ocupada se le pregunta a
 * GestorReservas y no se recalcula acá: es una regla de negocio, no una cuestión de
 * formato.
 */
public class VistasCartelera {

    public record PeliculaVista(int id, String titulo, int duracionMinutos, List<String> generos,
                                String clasificacion, String posterUrl, String director, int anio,
                                String idiomaOriginal, String sinopsis, boolean enCartelera) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FuncionVista(int id, int peliculaId, int salaId, String inicio, String idioma,
                               String proyeccion, double precio, double precioDesde, SalaVista sala,
                               PeliculaVista pelicula, List<AsientoVista> asientos, Integer libres) {
    }

    private final GestorCartelera cartelera;
    private final GestorSalas salas;
    private final GestorReservas reservas;
    private final CalculadoraPrecio calculadora;
    private final VistasSalas vistasSalas;

    public VistasCartelera(GestorCartelera cartelera, GestorSalas salas, GestorReservas reservas,
                           CalculadoraPrecio calculadora, VistasSalas vistasSalas) {
        this.cartelera = cartelera;
        this.salas = salas;
        this.reservas = reservas;
        this.calculadora = calculadora;
        this.vistasSalas = vistasSalas;
    }

    public PeliculaVista pelicula(Pelicula p) {
        return new PeliculaVista(p.getId(), p.getTitulo(), p.getDuracionMinutos(),
                p.getGeneros().stream().map(Enum::name).toList(),
                p.getClasificacion().name(), p.getPosterUrl(), p.getDirector(), p.getAnio(),
                p.getIdiomaOriginal(), p.getSinopsis(), p.estaEnCartelera());
    }

    /** Para el listado del cliente: la función con su sala, sin el mapa de butacas. */
    public FuncionVista funcion(Funcion f) {
        return armar(f, null, null, null);
    }

    /** Para el listado del encargado, que muestra qué película va en cada función. */
    public FuncionVista funcionConPelicula(Funcion f) {
        return armar(f, peliculaDe(f), null, null);
    }

    /** El mapa de butacas: cada asiento de la sala con su precio y si está tomado acá. */
    public FuncionVista funcionConButacas(Funcion f) {
        Sala sala = salaDe(f);
        Set<Integer> ocupados = reservas.asientosOcupados(f.getId());
        List<AsientoVista> butacas = salas.asientosDe(sala.getId()).stream()
                .map(a -> vistasSalas.asiento(a, f, sala, ocupados))
                .toList();
        return armar(f, peliculaDe(f), butacas, reservas.lugaresLibres(f.getId()));
    }

    private FuncionVista armar(Funcion f, PeliculaVista pelicula, List<AsientoVista> butacas,
                               Integer libres) {
        Sala sala = salaDe(f);
        List<Asiento> asientos = salas.asientosDe(sala.getId());
        return new FuncionVista(f.getId(), f.getPeliculaId(), f.getSalaId(),
                Fechas.texto(f.getInicio()), f.getVersion().name(), f.getProyeccion().name(),
                f.getPrecio(), calculadora.precioBaseEnSala(f, sala),
                vistasSalas.sala(sala, asientos), pelicula, butacas, libres);
    }

    private Sala salaDe(Funcion f) {
        return salas.buscar(f.getSalaId())
                .orElseThrow(() -> new NoEncontrado("No existe la sala " + f.getSalaId()));
    }

    private PeliculaVista peliculaDe(Funcion f) {
        return cartelera.buscar(f.getPeliculaId()).map(this::pelicula).orElse(null);
    }
}
