package ar.uade.cine.controller;

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

import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.controller.vistas.VistasCartelera;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.dto.funciones.PedidoFuncionDTO;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.RecursoNoEncontrado;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Funciones", description = "La programación de una función y su mapa de butacas")
@RestController
public class FuncionController {

    private final GestorFunciones funciones;
    private final VistasCartelera vistas;

    public FuncionController(GestorFunciones funciones, VistasCartelera vistas) {
        this.funciones = funciones;
        this.vistas = vistas;
    }

    @Operation(summary = "Buscar funciones por película, sala y rango de fechas")
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

    @Operation(summary = "Una función con su mapa de butacas y el precio ya calculado de cada una")
    @GetMapping("/api/funciones/{id}")
    public FuncionVistaDTO detalle(@PathVariable int id,
                                   @RequestParam(required = false) String sesion) {
        return vistas.funcionConButacas(buscar(id), sesion);
    }

    @Operation(summary = "Programar una función")
    @PostMapping("/api/funciones")
    @ResponseStatus(HttpStatus.CREATED)
    public FuncionVistaDTO programar(@Valid @RequestBody PedidoFuncionDTO pedido) {
        Funcion funcion = funciones.programar(pedido.peliculaId(), pedido.salaId(),
                Parseo.momento(pedido.inicio(), "la fecha y hora"),
                Parseo.constante(Version.class, pedido.idioma(), "el idioma"),
                Parseo.constante(Proyeccion.class, pedido.proyeccion(), "la proyección"),
                Dinero.de(pedido.precio()));
        return vistas.funcionConPelicula(funcion);
    }

    @Operation(summary = "Borrar una función")
    @DeleteMapping("/api/funciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) {
        buscar(id);
        funciones.eliminar(id);
    }

    private Funcion buscar(int id) {
        return funciones.buscar(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la función " + id));
    }
}
