package ar.uade.cine.api;

import java.util.Comparator;
import java.util.List;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.servicio.DatosPelicula;
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
                          VistasCartelera vistas) {

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
            ctx.status(HttpStatus.CREATED).json(vistas.pelicula(cartelera.agregar(datosDe(pedido))));
        });

        app.put("/api/peliculas/{id}", ctx -> {
            int id = Parseo.id(ctx);
            // El gestor rechaza el id inexistente como dato inválido; acá se pregunta
            // antes para poder responder 404 y no 400.
            buscar(cartelera, id);
            PedidoPelicula pedido = ctx.bodyAsClass(PedidoPelicula.class);
            ctx.json(vistas.pelicula(cartelera.editar(id, datosDe(pedido))));
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
    private static DatosPelicula datosDe(PedidoPelicula pedido) {
        return new DatosPelicula(pedido.titulo(), pedido.duracionMinutos(),
                pedido.generos() == null
                        ? null : Parseo.constantes(Genero.class, pedido.generos(), "el género"),
                pedido.clasificacion() == null
                        ? null : Parseo.constante(Clasificacion.class, pedido.clasificacion(),
                                "la clasificación"),
                pedido.director(), pedido.sinopsis(), pedido.anio(), pedido.idiomaOriginal(),
                pedido.posterUrl(), pedido.enCartelera());
    }
}
