package ar.uade.cine.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;

import ar.uade.cine.Aplicacion;
import ar.uade.cine.api.http.NoEncontrado;
import ar.uade.cine.api.rutas.Rutas;
import ar.uade.cine.dto.ErrorVistaDTO;
import ar.uade.cine.persistencia.ButacaOcupadaException;
import ar.uade.cine.persistencia.PersistenciaException;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

/**
 * La aplicación hablada por HTTP. La levanta con {@link Aplicacion} y después no hace más
 * que traducir: cada ruta arma una llamada a un gestor y devuelve lo que responde.
 *
 * <p>Acá no se nombra ninguna implementación concreta de persistencia: esta clase ni se
 * entera de que atrás hay MySQL. Y desde que el cableado de rutas vive en {@link Rutas},
 * tampoco sabe qué endpoints existen: le quedan dos trabajos, configurar el servidor y
 * traducir los errores del negocio a códigos HTTP.
 *
 * <p>Ninguna regla vive acá. Cuando un gestor rechaza algo lo hace con
 * IllegalArgumentException, y esta capa la convierte en un 400 con el mensaje intacto,
 * porque es el texto que el front le muestra al usuario.
 */
public class ServidorApi {

    private static final Logger LOG = LoggerFactory.getLogger(ServidorApi.class);
    private static final int PUERTO_POR_DEFECTO = 8080;

    public static void main(String[] args) {
        armar(Aplicacion.enMySQL()).start(puerto());
    }

    /**
     * La API entera, armada y sin arrancar: el JSON, las rutas y la traducción de errores.
     *
     * <p>Está separada de {@link #main} porque son dos cosas distintas —armar y escuchar en
     * un puerto— y solo la segunda necesita MySQL. Con el armado aparte, un test puede
     * levantar <strong>esta misma</strong> API contra la aplicación en memoria y en un
     * puerto libre. Si el test armara su propio Javalin, estaría probando una API parecida:
     * el día que acá se agregue un manejador de errores, el de la prueba seguiría
     * respondiendo como antes y nadie se enteraría.
     */
    static Javalin armar(Aplicacion aplicacion) {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            // Un campo de más en el pedido no es motivo para rechazarlo: el front puede
            // mandar algo que este backend todavía no usa.
            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)));
        });

        Rutas.registrarTodas(app, aplicacion);
        registrarErrores(app);
        return app;
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
                ctx.json(new ErrorVistaDTO("No existe la ruta " + ctx.path()));
            }
        });
    }

    private static void responder(io.javalin.http.Context ctx, HttpStatus estado, String mensaje) {
        ctx.attribute("respondido", true);
        ctx.status(estado).json(new ErrorVistaDTO(mensaje));
    }

    private static int puerto() {
        String valor = System.getenv("PORT");
        return valor == null || valor.isBlank() ? PUERTO_POR_DEFECTO : Integer.parseInt(valor);
    }
}
