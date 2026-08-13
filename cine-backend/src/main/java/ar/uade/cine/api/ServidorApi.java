package ar.uade.cine.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;

import ar.uade.cine.Aplicacion;
import ar.uade.cine.persistencia.ButacaOcupadaException;
import ar.uade.cine.persistencia.PersistenciaException;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

/**
 * La misma aplicación, hablada por HTTP en vez de por consola. La levanta con
 * {@link Aplicacion}, igual que Main, y después no hace más que traducir: cada ruta arma
 * una llamada a un gestor y devuelve lo que responde.
 *
 * <p>Que las dos puertas compartan ese armado es lo que garantiza que sean la misma
 * aplicación y no dos parecidas. Acá no se nombra ninguna implementación concreta de
 * persistencia: esta clase ni se entera de que atrás hay MySQL.
 *
 * <p>Ninguna regla vive acá. Cuando un gestor rechaza algo lo hace con
 * IllegalArgumentException, y esta capa la convierte en un 400 con el mensaje intacto,
 * porque es el texto que el front le muestra al usuario.
 */
public class ServidorApi {

    private static final Logger LOG = LoggerFactory.getLogger(ServidorApi.class);
    private static final int PUERTO_POR_DEFECTO = 8080;

    /** La forma de cualquier error, sea 400, 404 o 500. */
    record ErrorVista(String error) {
    }

    public static void main(String[] args) {
        Aplicacion aplicacion = Aplicacion.enMySQL();

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            // Un campo de más en el pedido no es motivo para rechazarlo: el front puede
            // mandar algo que este backend todavía no usa.
            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)));
        });

        registrarRutas(app, aplicacion);
        registrarErrores(app);
        app.start(puerto());
    }

    /** Qué gestor atiende cada familia de rutas. */
    private static void registrarRutas(Javalin app, Aplicacion aplicacion) {
        Vistas vistas = new Vistas(aplicacion.getCartelera(), aplicacion.getSalas(),
                aplicacion.getFunciones(), aplicacion.getReservas(), aplicacion.getPagos(),
                aplicacion.getClientes());

        RutasCatalogos.registrar(app);
        RutasPeliculas.registrar(app, aplicacion.getCartelera(), aplicacion.getFunciones(), vistas);
        RutasSalas.registrar(app, aplicacion.getSalas(), vistas);
        RutasFunciones.registrar(app, aplicacion.getFunciones(), vistas);
        RutasClientes.registrar(app, aplicacion.getClientes(), vistas);
        RutasReservas.registrar(app, aplicacion.getReservas(), aplicacion.getClientes(), vistas);
        RutasPromociones.registrar(app, aplicacion.getPromociones(), vistas);
        RutasPagos.registrar(app, aplicacion.getPagos(), aplicacion.getReservas(), vistas);
        RutasSesion.registrar(app, aplicacion.getEmpleados(), vistas);
    }

    private static void registrarErrores(Javalin app) {
        // Lo que rechaza un gestor: dato inválido o regla de negocio incumplida.
        app.exception(IllegalArgumentException.class, (e, ctx) ->
                responder(ctx, HttpStatus.BAD_REQUEST, e.getMessage()));

        app.exception(NoEncontrado.class, (e, ctx) ->
                responder(ctx, HttpStatus.NOT_FOUND, e.getMessage()));

        // Perder una carrera por la última butaca no es una falla: es el resultado
        // legítimo de que otro haya confirmado primero. Por eso 409 y no 500, y el
        // mensaje se muestra tal cual —el front vuelve a pedir el mapa y repinta.
        app.exception(ButacaOcupadaException.class, (e, ctx) ->
                responder(ctx, HttpStatus.CONFLICT, e.getMessage()));

        // La base caída o una consulta rota no son culpa de quien llama, y el detalle
        // interno no le sirve: va al log del servidor, no a la respuesta.
        app.exception(PersistenciaException.class, (e, ctx) -> {
            LOG.error("Falló el acceso a los datos en {} {}", ctx.method(), ctx.path(), e);
            responder(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo acceder a los datos");
        });

        app.error(HttpStatus.NOT_FOUND, ctx -> {
            if (ctx.attribute("respondido") == null) {
                ctx.json(new ErrorVista("No existe la ruta " + ctx.path()));
            }
        });
    }

    private static void responder(io.javalin.http.Context ctx, HttpStatus estado, String mensaje) {
        ctx.attribute("respondido", true);
        ctx.status(estado).json(new ErrorVista(mensaje));
    }

    private static int puerto() {
        String valor = System.getenv("PORT");
        return valor == null || valor.isBlank() ? PUERTO_POR_DEFECTO : Integer.parseInt(valor);
    }
}
