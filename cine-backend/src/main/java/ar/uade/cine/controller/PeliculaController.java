package ar.uade.cine.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.controller.vistas.VistasCartelera;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.dto.cartelera.PedidoPeliculaDTO;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.RecursoNoEncontrado;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Películas", description = "La cartelera pública y el ABM del catálogo")
@RestController
public class PeliculaController {

    private final GestorCartelera cartelera;
    private final GestorRevisionCartelera revision;
    private final GestorFunciones funciones;
    private final VistasCartelera vistas;

    public PeliculaController(GestorCartelera cartelera, GestorRevisionCartelera revision,
                                GestorFunciones funciones, VistasCartelera vistas) {
        this.cartelera = cartelera;
        this.revision = revision;
        this.funciones = funciones;
        this.vistas = vistas;
    }

    @Operation(summary = "La cartelera pública: solo lo que está en exhibición")
    @GetMapping("/api/cartelera")
    public List<PeliculaVistaDTO> cartelera(@RequestParam(required = false) String genero) {
        return cartelera.listarEnCartelera(Parseo.constanteOpcional(Genero.class, genero, "el género"))
                .stream().map(vistas::pelicula).toList();
    }

    @Operation(summary = "Buscar en el catálogo entero, esté o no en cartelera")
    @GetMapping("/api/peliculas")
    public List<PeliculaVistaDTO> buscar(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String genero,
                                         @RequestParam(required = false) String publicada) {
        return cartelera.buscar(q,
                        Parseo.constanteOpcional(Genero.class, genero, "el género"),
                        Parseo.booleanOpcional(publicada, "publicada"))
                .stream().map(vistas::pelicula).toList();
    }

    @Operation(summary = "El buzón: lo que trajo el importador y todavía nadie revisó")
    @GetMapping("/api/peliculas/pendientes")
    public List<PeliculaVistaDTO> pendientes() {
        return revision.listarPendientes().stream().map(vistas::pelicula).toList();
    }

    @Operation(summary = "El detalle de una película")
    @GetMapping("/api/peliculas/{id}")
    public PeliculaVistaDTO detalle(@PathVariable int id) {
        return vistas.pelicula(buscar(id));
    }

    @Operation(summary = "Las funciones programadas de una película")
    @GetMapping("/api/peliculas/{id}/funciones")
    public List<FuncionVistaDTO> funcionesDe(@PathVariable int id) {
        buscar(id);
        return funciones.listarPorPelicula(id).stream()
                .sorted(Comparator.comparing(Funcion::getInicio))
                .map(vistas::funcion)
                .toList();
    }

    @Operation(summary = "Dar de alta una película a mano")
    @PostMapping("/api/peliculas")
    @ResponseStatus(HttpStatus.CREATED)
    public PeliculaVistaDTO agregar(@RequestBody PedidoPeliculaDTO pedido) {
        return vistas.pelicula(cartelera.agregar(datosDe(pedido)));
    }

    @Operation(summary = "Alta del importador: entra al buzón, no al catálogo")
    @PostMapping("/api/peliculas/importadas")
    @ResponseStatus(HttpStatus.CREATED)
    public PeliculaVistaDTO importar(@RequestBody PedidoPeliculaDTO pedido) {
        return vistas.pelicula(revision.importar(datosDe(pedido)));
    }

    @Operation(summary = "Aceptar una película del buzón y publicarla")
    @PostMapping("/api/peliculas/{id}/confirmacion")
    public PeliculaVistaDTO confirmar(@PathVariable int id) {
        buscar(id);
        return vistas.pelicula(revision.confirmar(id));
    }

    @Operation(summary = "Descartar una película del buzón")
    @PostMapping("/api/peliculas/{id}/descarte")
    public PeliculaVistaDTO descartar(@PathVariable int id) {
        buscar(id);
        return vistas.pelicula(revision.descartar(id));
    }

    @Operation(summary = "Editar una película")
    @PutMapping("/api/peliculas/{id}")
    public PeliculaVistaDTO editar(@PathVariable int id, @RequestBody PedidoPeliculaDTO pedido) {
        // Se busca antes para responder 404 y no el 400 del gestor.
        buscar(id);
        return vistas.pelicula(cartelera.editar(id, datosDe(pedido)));
    }

    @Operation(summary = "Borrar una película del catálogo")
    @DeleteMapping("/api/peliculas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) {
        buscar(id);
        cartelera.eliminar(id);
    }

    private Pelicula buscar(int id) {
        return cartelera.buscar(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la película " + id));
    }

    private static DatosPelicula datosDe(PedidoPeliculaDTO pedido) {
        return new DatosPelicula(pedido.titulo(), pedido.duracionMinutos(),
                pedido.generos() == null
                        ? null : Parseo.constantes(Genero.class, pedido.generos(), "el género"),
                pedido.clasificacion() == null
                        ? null : Parseo.constante(Clasificacion.class, pedido.clasificacion(),
                                "la clasificación"),
                pedido.director(), pedido.sinopsis(), pedido.anio(), pedido.idiomaOriginal(),
                pedido.posterUrl(), pedido.enCartelera(), pedido.puntaje(), pedido.votos());
    }
}
