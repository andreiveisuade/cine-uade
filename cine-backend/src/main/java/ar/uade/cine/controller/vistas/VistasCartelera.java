package ar.uade.cine.controller.vistas;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.service.ventas.CalculadoraPrecio;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.ventas.Ocupacion;
import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.http.NoEncontrado;

@Component
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
                p.getEstadoRevision().name(), p.getPuntaje(), p.getVotos());
    }

    public FuncionVistaDTO funcion(Funcion f) {
        return armar(f, null, null, null);
    }

    public FuncionVistaDTO funcionConPelicula(Funcion f) {
        return armar(f, peliculaDe(f), null, null);
    }

    public FuncionVistaDTO funcionConButacas(Funcion f) {
        return funcionConButacas(f, null);
    }

    public FuncionVistaDTO funcionConButacas(Funcion f, String sesion) {
        Sala sala = salaDe(f);
        List<Asiento> asientos = salas.asientosDe(sala.getId());
        Set<Integer> ocupados = ocupacion.asientosOcupados(f.getId(), sesion);
        List<AsientoVistaDTO> butacas = asientos.stream()
                .map(a -> vistasSalas.asiento(a, f, sala, ocupados))
                .toList();
        int libres = Ocupacion.libresEntre(asientos, ocupados).size();
        return armar(f, sala, asientos, peliculaDe(f), butacas, libres);
    }

    private FuncionVistaDTO armar(Funcion f, PeliculaVistaDTO pelicula, List<AsientoVistaDTO> butacas,
                                  Integer libres) {
        Sala sala = salaDe(f);
        return armar(f, sala, salas.asientosDe(sala.getId()), pelicula, butacas, libres);
    }

    private FuncionVistaDTO armar(Funcion f, Sala sala, List<Asiento> asientos, PeliculaVistaDTO pelicula,
                                  List<AsientoVistaDTO> butacas, Integer libres) {
        return new FuncionVistaDTO(f.getId(), f.getPeliculaId(), f.getSalaId(),
                Fechas.texto(f.getInicio()), f.getVersion().name(), f.getProyeccion().name(),
                f.getPrecio().aPesos(), calculadora.precioBaseEnSala(f, sala).aPesos(),
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
