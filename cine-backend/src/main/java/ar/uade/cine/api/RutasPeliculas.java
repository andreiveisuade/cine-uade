package ar.uade.cine.api;

import java.util.Comparator;
import java.util.List;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.cartelera.PeliculaImpl;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorFunciones;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * Cartelera y ABM de películas. Los campos que faltan en el pedido se mandan igual al
 * gestor —en null o en cero— para que el mensaje de error sea el suyo y no uno que
 * invente esta capa: el front lo muestra tal cual al usuario.
 */
class RutasPeliculas {

    /** Sirve para el alta y para la edición: en el PUT todos los campos son opcionales. */
    record PedidoPelicula(String titulo, Integer duracionMinutos, List<String> generos,
                          String clasificacion, String director, String sinopsis, Integer anio,
                          String idiomaOriginal, String posterUrl, Boolean enCartelera) {
    }

    static void registrar(Javalin app, GestorCartelera cartelera, GestorFunciones funciones,
                          Vistas vistas) {

        // Lo que ve el cliente: solo lo que está en exhibición (CU-01b para el filtro).
        app.get("/api/cartelera", ctx -> {
            List<Pelicula> peliculas = cartelera.listarEnCartelera();
            String genero = ctx.queryParam("genero");
            if (genero != null && !genero.isBlank()) {
                Genero buscado = Parseo.constante(Genero.class, genero, "el género");
                peliculas = peliculas.stream().filter(p -> p.getGeneros().contains(buscado)).toList();
            }
            ctx.json(peliculas.stream().map(vistas::pelicula).toList());
        });

        // Lo que ve el encargado: el catálogo entero, esté o no en cartelera.
        app.get("/api/peliculas", ctx ->
                ctx.json(cartelera.listar().stream().map(vistas::pelicula).toList()));

        app.get("/api/peliculas/{id}", ctx ->
                ctx.json(vistas.pelicula(buscar(cartelera, Parseo.id(ctx)))));

        app.get("/api/peliculas/{id}/funciones", ctx -> {
            int id = Parseo.id(ctx);
            buscar(cartelera, id);
            ctx.json(funciones.listarPorPelicula(id).stream()
                    .sorted(Comparator.comparing(Funcion::getInicio))
                    .map(vistas::funcion)
                    .toList());
        });

        app.post("/api/peliculas", ctx -> {
            PedidoPelicula pedido = ctx.bodyAsClass(PedidoPelicula.class);
            Pelicula pelicula = cartelera.agregar(pedido.titulo(),
                    pedido.duracionMinutos() == null ? 0 : pedido.duracionMinutos(),
                    Parseo.constantes(Genero.class, pedido.generos(), "el género"),
                    clasificacionDe(pedido, null));
            // El alta solo recibe lo que hace a la película; el catálogo va por setter y
            // necesita un guardado más.
            aplicarCatalogo(pelicula, pedido);
            cartelera.actualizar(pelicula);
            ctx.status(HttpStatus.CREATED).json(vistas.pelicula(pelicula));
        });

        app.put("/api/peliculas/{id}", ctx -> {
            int id = Parseo.id(ctx);
            Pelicula actual = buscar(cartelera, id);
            PedidoPelicula pedido = ctx.bodyAsClass(PedidoPelicula.class);

            // Pelicula no tiene setTitulo: para editarla se arma una nueva con el mismo
            // id y lo que no vino en el pedido se copia de la que ya estaba.
            Pelicula editada = new PeliculaImpl(id,
                    pedido.titulo() == null ? actual.getTitulo() : pedido.titulo(),
                    pedido.duracionMinutos() == null ? actual.getDuracionMinutos() : pedido.duracionMinutos(),
                    clasificacionDe(pedido, actual.getClasificacion()));
            List<Genero> generos = pedido.generos() == null
                    ? actual.getGeneros()
                    : Parseo.constantes(Genero.class, pedido.generos(), "el género");
            generos.forEach(editada::agregarGenero);
            copiarCatalogo(actual, editada);
            aplicarCatalogo(editada, pedido);

            cartelera.actualizar(editada);
            ctx.json(vistas.pelicula(editada));
        });

        app.delete("/api/peliculas/{id}", ctx -> {
            int id = Parseo.id(ctx);
            buscar(cartelera, id);
            cartelera.eliminar(id);
            ctx.status(HttpStatus.NO_CONTENT);
        });
    }

    private static Pelicula buscar(GestorCartelera cartelera, int id) {
        return cartelera.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la película " + id));
    }

    /** Sin clasificación en el pedido vale la de antes; en el alta, el null que valida R10. */
    private static Clasificacion clasificacionDe(PedidoPelicula pedido, Clasificacion actual) {
        return pedido.clasificacion() == null
                ? actual
                : Parseo.constante(Clasificacion.class, pedido.clasificacion(), "la clasificación");
    }

    private static void copiarCatalogo(Pelicula desde, Pelicula hacia) {
        hacia.setDirector(desde.getDirector());
        hacia.setSinopsis(desde.getSinopsis());
        hacia.setAnio(desde.getAnio());
        hacia.setIdiomaOriginal(desde.getIdiomaOriginal());
        hacia.setPosterUrl(desde.getPosterUrl());
        hacia.setEnCartelera(desde.estaEnCartelera());
    }

    private static void aplicarCatalogo(Pelicula pelicula, PedidoPelicula pedido) {
        if (pedido.director() != null) {
            pelicula.setDirector(pedido.director());
        }
        if (pedido.sinopsis() != null) {
            pelicula.setSinopsis(pedido.sinopsis());
        }
        if (pedido.anio() != null) {
            pelicula.setAnio(pedido.anio());
        }
        if (pedido.idiomaOriginal() != null) {
            pelicula.setIdiomaOriginal(pedido.idiomaOriginal());
        }
        if (pedido.posterUrl() != null) {
            pelicula.setPosterUrl(pedido.posterUrl());
        }
        if (pedido.enCartelera() != null) {
            pelicula.setEnCartelera(pedido.enCartelera());
        }
    }
}
