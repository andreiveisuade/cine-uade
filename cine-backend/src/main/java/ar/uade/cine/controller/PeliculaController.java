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

import ar.uade.cine.controller.http.NoEncontrado;
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

/**
 * Cartelera y ABM de películas. Los campos que faltan en el pedido se mandan igual al
 * gestor —en null o en cero— para que el mensaje de error sea el suyo y no uno que
 * invente esta capa: el front lo muestra tal cual al usuario.
 *
 * <p>Los tres gestores y la vista entran por constructor y los pone el contenedor. Que un
 * controlador reciba <strong>solo</strong> lo que usa se sigue cuidando igual que cuando
 * se los pasaba a mano: pedir menos colaboradores es lo que hace evidente qué depende de
 * qué.
 */
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

    /** Lo que ve el cliente: solo lo que está en exhibición (CU-01b para el filtro). */
    @GetMapping("/api/cartelera")
    public List<PeliculaVistaDTO> cartelera(@RequestParam(required = false) String genero) {
        List<Pelicula> peliculas = cartelera.listarEnCartelera();
        if (genero != null && !genero.isBlank()) {
            Genero buscado = Parseo.constante(Genero.class, genero, "el género");
            peliculas = peliculas.stream().filter(p -> p.getGeneros().contains(buscado)).toList();
        }
        return peliculas.stream().map(vistas::pelicula).toList();
    }

    /**
     * Lo que ve el encargado: el catálogo entero, esté o no en cartelera. El catálogo dejó
     * de ser cuatro títulos de prueba desde que el importador trae la cartelera real:
     * filtrar por título es lo primero que hace falta.
     */
    @GetMapping("/api/peliculas")
    public List<PeliculaVistaDTO> buscar(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String genero,
                                         @RequestParam(required = false) String publicada) {
        return cartelera.buscar(q,
                        Parseo.constanteOpcional(Genero.class, genero, "el género"),
                        Parseo.booleanOpcional(publicada, "publicada"))
                .stream().map(vistas::pelicula).toList();
    }

    /**
     * El buzón: lo que trajo el importador y todavía nadie miró. Con Javalin había que
     * registrarlo antes que {@code /{id}} porque resolvía por orden; Spring elige la ruta
     * más específica, así que el orden de los métodos ya no decide nada.
     */
    @GetMapping("/api/peliculas/pendientes")
    public List<PeliculaVistaDTO> pendientes() {
        return revision.listarPendientes().stream().map(vistas::pelicula).toList();
    }

    @GetMapping("/api/peliculas/{id}")
    public PeliculaVistaDTO detalle(@PathVariable int id) {
        return vistas.pelicula(buscar(id));
    }

    @GetMapping("/api/peliculas/{id}/funciones")
    public List<FuncionVistaDTO> funcionesDe(@PathVariable int id) {
        buscar(id);
        return funciones.listarPorPelicula(id).stream()
                .sorted(Comparator.comparing(Funcion::getInicio))
                .map(vistas::funcion)
                .toList();
    }

    @PostMapping("/api/peliculas")
    @ResponseStatus(HttpStatus.CREATED)
    public PeliculaVistaDTO agregar(@RequestBody PedidoPeliculaDTO pedido) {
        return vistas.pelicula(cartelera.agregar(datosDe(pedido)));
    }

    /**
     * El alta del importador, separada de la del encargado: lo que baja de TMDB es una
     * propuesta y entra al buzón, no al catálogo. Es la misma forma de pedido, así que el
     * importador no tiene que aprender otro cuerpo, solo otra URL.
     */
    @PostMapping("/api/peliculas/importadas")
    @ResponseStatus(HttpStatus.CREATED)
    public PeliculaVistaDTO importar(@RequestBody PedidoPeliculaDTO pedido) {
        return vistas.pelicula(revision.importar(datosDe(pedido)));
    }

    /**
     * Confirmar y descartar son POST y no PUT por lo mismo que la baja de una promoción:
     * no se está mandando un estado nuevo, se está tomando una decisión sobre la película.
     */
    @PostMapping("/api/peliculas/{id}/confirmacion")
    public PeliculaVistaDTO confirmar(@PathVariable int id) {
        buscar(id);
        return vistas.pelicula(revision.confirmar(id));
    }

    @PostMapping("/api/peliculas/{id}/descarte")
    public PeliculaVistaDTO descartar(@PathVariable int id) {
        buscar(id);
        return vistas.pelicula(revision.descartar(id));
    }

    @PutMapping("/api/peliculas/{id}")
    public PeliculaVistaDTO editar(@PathVariable int id, @RequestBody PedidoPeliculaDTO pedido) {
        // El gestor rechaza el id inexistente como dato inválido; acá se pregunta antes
        // para poder responder 404 y no 400.
        buscar(id);
        return vistas.pelicula(cartelera.editar(id, datosDe(pedido)));
    }

    @DeleteMapping("/api/peliculas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) {
        buscar(id);
        cartelera.eliminar(id);
    }

    private Pelicula buscar(int id) {
        return cartelera.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la película " + id));
    }

    /**
     * Lo único que hace esta capa con el pedido: pasar el texto que llegó a los tipos del
     * dominio. Qué campos son obligatorios, cuáles pisan a los guardados y cuáles se
     * conservan lo decide el gestor, que es donde ese criterio sirve para las dos
     * interfaces.
     *
     * <p>Un campo ausente viaja en null a propósito: es lo que el gestor lee como "no lo
     * mandé". En el alta, ese mismo null es el que dispara el error de dato faltante, con
     * el mensaje del gestor y no con uno que invente esta capa.
     */
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
