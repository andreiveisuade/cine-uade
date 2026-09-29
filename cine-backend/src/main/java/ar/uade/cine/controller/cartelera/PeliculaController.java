package ar.uade.cine.controller.cartelera;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.dto.cartelera.PedidoEdicionPeliculaDTO;
import ar.uade.cine.dto.cartelera.PedidoPeliculaDTO;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.RecursoNoEncontrado;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/cartelera y /api/peliculas: cartelera pública y ABM; traduce HTTP a GestorCartelera.
// El buzón de revisión comparte el tag pero tiene su propio controller: RevisionController.
@Tag(name = "Películas", description = "La cartelera pública y el ABM del catálogo")
@RestController
@RequiredArgsConstructor
public class PeliculaController {

    private final GestorCartelera cartelera;
    private final GestorFunciones funciones;
    private final VistasCartelera vistas;

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

    @Operation(summary = "El detalle de una película")
    @GetMapping("/api/peliculas/{id}")
    public PeliculaVistaDTO detalle(@PathVariable int id) {
        return vistas.pelicula(buscar(id));
    }

    @Operation(summary = "Las funciones programadas de una película")
    @GetMapping("/api/peliculas/{id}/funciones")
    public List<FuncionVistaDTO> funcionesDe(@PathVariable int id) {
        buscar(id);
        return vistas.funciones(funciones.listarPorPelicula(id));
    }

    @Operation(summary = "Dar de alta una película a mano")
    @PostMapping("/api/peliculas")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PeliculaVistaDTO> agregar(@Valid @RequestBody PedidoPeliculaDTO pedido) {
        return creada(cartelera.agregar(datosDe(pedido)));
    }

    @Operation(summary = "Editar una película")
    @PutMapping("/api/peliculas/{id}")
    public PeliculaVistaDTO editar(@PathVariable int id, @RequestBody PedidoEdicionPeliculaDTO pedido) {
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

    private ResponseEntity<PeliculaVistaDTO> creada(Pelicula pelicula) {
        return Creado.en("/api/peliculas/" + pelicula.getId(), vistas.pelicula(pelicula));
    }

    private Pelicula buscar(int id) {
        return cartelera.buscar(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la película " + id));
    }

    private static DatosPelicula datosDe(PedidoPeliculaDTO pedido) {
        return datosDe(new PedidoEdicionPeliculaDTO(pedido.titulo(), pedido.duracionMinutos(),
                pedido.generos(), pedido.clasificacion(), pedido.director(), pedido.sinopsis(),
                pedido.anio(), pedido.idiomaOriginal(), pedido.posterUrl(), pedido.enCartelera(),
                pedido.puntaje(), pedido.votos()));
    }

    private static DatosPelicula datosDe(PedidoEdicionPeliculaDTO pedido) {
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
