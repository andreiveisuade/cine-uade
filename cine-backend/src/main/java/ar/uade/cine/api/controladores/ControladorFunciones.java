package ar.uade.cine.api.controladores;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.api.http.NoEncontrado;
import ar.uade.cine.api.http.Parseo;
import ar.uade.cine.api.vistas.VistasCartelera;
import ar.uade.cine.dominio.dinero.Dinero;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.dto.funciones.PedidoFuncionDTO;
import ar.uade.cine.servicio.funciones.GestorFunciones;

/**
 * Programación de funciones y mapa de butacas. El detalle de una función es el endpoint
 * con el que el cliente elige dónde sentarse: por eso trae cada asiento con su precio ya
 * calculado y si está tomado en esa proyección.
 */
@RestController
public class ControladorFunciones {

    private final GestorFunciones funciones;
    private final VistasCartelera vistas;

    public ControladorFunciones(GestorFunciones funciones, VistasCartelera vistas) {
        this.funciones = funciones;
        this.vistas = vistas;
    }

    /**
     * Es la lista más larga del sistema: una semana de seis salas pasa de cien funciones.
     * Los filtros son los que usa quien programa.
     */
    @GetMapping("/api/funciones")
    public List<FuncionVistaDTO> buscar(@RequestParam(required = false) String peliculaId,
                                        @RequestParam(required = false) String salaId,
                                        @RequestParam(required = false) String desde,
                                        @RequestParam(required = false) String hasta) {
        return funciones.buscar(
                        Parseo.numeroOpcional(peliculaId, "la película"),
                        Parseo.numeroOpcional(salaId, "la sala"),
                        Parseo.diaOpcional(desde, "la fecha de inicio"),
                        Parseo.diaOpcional(hasta, "la fecha de fin"))
                .stream()
                .sorted(Comparator.comparing(Funcion::getInicio))
                .map(vistas::funcionConPelicula)
                .toList();
    }

    /**
     * {@code sesion} es opcional y solo cambia una cosa: las butacas que esa sesión tiene
     * bloqueadas mientras elige no le vuelven marcadas como ocupadas a ella misma.
     */
    @GetMapping("/api/funciones/{id}")
    public FuncionVistaDTO detalle(@PathVariable int id,
                                   @RequestParam(required = false) String sesion) {
        return vistas.funcionConButacas(buscar(id), sesion);
    }

    @PostMapping("/api/funciones")
    @ResponseStatus(HttpStatus.CREATED)
    public FuncionVistaDTO programar(@RequestBody PedidoFuncionDTO pedido) {
        Funcion funcion = funciones.programar(
                pedido.peliculaId() == null ? 0 : pedido.peliculaId(),
                pedido.salaId() == null ? 0 : pedido.salaId(),
                pedido.inicio() == null ? null : Parseo.momento(pedido.inicio(), "la fecha y hora"),
                pedido.idioma() == null
                        ? null : Parseo.constante(Version.class, pedido.idioma(), "el idioma"),
                pedido.proyeccion() == null
                        ? null : Parseo.constante(Proyeccion.class, pedido.proyeccion(), "la proyección"),
                Dinero.de(pedido.precio() == null ? 0 : pedido.precio()));
        return vistas.funcionConPelicula(funcion);
    }

    @DeleteMapping("/api/funciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) {
        buscar(id);
        funciones.eliminar(id);
    }

    private Funcion buscar(int id) {
        return funciones.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la función " + id));
    }
}
