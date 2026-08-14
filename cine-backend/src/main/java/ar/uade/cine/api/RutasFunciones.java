package ar.uade.cine.api;

import java.util.Comparator;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dto.funciones.PedidoFuncionDTO;
import ar.uade.cine.servicio.GestorFunciones;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * Programación de funciones y mapa de butacas. El detalle de una función es el endpoint
 * con el que el cliente elige dónde sentarse: por eso trae cada asiento con su precio ya
 * calculado y si está tomado en esa proyección.
 */
class RutasFunciones {

    static void registrar(Javalin app, GestorFunciones funciones, VistasCartelera vistas) {

        // Es la lista más larga del sistema: una semana de seis salas pasa de cien
        // funciones. Los filtros son los que usa quien programa.
        app.get("/api/funciones", ctx ->
                ctx.json(funciones.buscar(
                                Parseo.numeroOpcional(ctx.queryParam("peliculaId"), "la película"),
                                Parseo.numeroOpcional(ctx.queryParam("salaId"), "la sala"),
                                Parseo.diaOpcional(ctx.queryParam("desde"), "la fecha de inicio"),
                                Parseo.diaOpcional(ctx.queryParam("hasta"), "la fecha de fin"))
                        .stream()
                        .sorted(Comparator.comparing(Funcion::getInicio))
                        .map(vistas::funcionConPelicula)
                        .toList()));

        // `sesion` es opcional y solo cambia una cosa: las butacas que esa sesión tiene
        // bloqueadas mientras elige no le vuelven marcadas como ocupadas a ella misma.
        app.get("/api/funciones/{id}", ctx ->
                ctx.json(vistas.funcionConButacas(buscar(funciones, Parseo.id(ctx)),
                        ctx.queryParam("sesion"))));

        app.post("/api/funciones", ctx -> {
            PedidoFuncionDTO pedido = ctx.bodyAsClass(PedidoFuncionDTO.class);
            Funcion funcion = funciones.programar(
                    pedido.peliculaId() == null ? 0 : pedido.peliculaId(),
                    pedido.salaId() == null ? 0 : pedido.salaId(),
                    pedido.inicio() == null ? null : Parseo.momento(pedido.inicio(), "la fecha y hora"),
                    pedido.idioma() == null ? null : Parseo.constante(Version.class, pedido.idioma(), "el idioma"),
                    pedido.proyeccion() == null ? null
                            : Parseo.constante(Proyeccion.class, pedido.proyeccion(), "la proyección"),
                    pedido.precio() == null ? 0 : pedido.precio());
            ctx.status(HttpStatus.CREATED).json(vistas.funcionConPelicula(funcion));
        });

        app.delete("/api/funciones/{id}", ctx -> {
            int id = Parseo.id(ctx);
            buscar(funciones, id);
            funciones.eliminar(id);
            ctx.status(HttpStatus.NO_CONTENT);
        });
    }

    private static Funcion buscar(GestorFunciones funciones, int id) {
        return funciones.buscar(id).orElseThrow(() -> new NoEncontrado("No existe la función " + id));
    }
}
