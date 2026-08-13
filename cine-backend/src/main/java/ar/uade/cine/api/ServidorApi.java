package ar.uade.cine.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;

import ar.uade.cine.persistencia.EmpleadoDAO;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ButacaOcupadaException;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.GeneradorTicket;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.PersistenciaException;
import ar.uade.cine.persistencia.PromocionDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.persistencia.archivo.GeneradorTicketTxt;
import ar.uade.cine.persistencia.mysql.EmpleadoDAOMySQL;
import ar.uade.cine.persistencia.mysql.AsientoDAOMySQL;
import ar.uade.cine.persistencia.mysql.ClienteDAOMySQL;
import ar.uade.cine.persistencia.mysql.CompraCandyDAOMySQL;
import ar.uade.cine.persistencia.mysql.FuncionDAOMySQL;
import ar.uade.cine.persistencia.mysql.PagoDAOMySQL;
import ar.uade.cine.persistencia.mysql.PeliculaDAOMySQL;
import ar.uade.cine.persistencia.mysql.PromocionDAOMySQL;
import ar.uade.cine.persistencia.mysql.ReservaDAOMySQL;
import ar.uade.cine.persistencia.mysql.SalaDAOMySQL;
import ar.uade.cine.servicio.GestorEmpleados;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorPromociones;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

/**
 * La misma aplicación, hablada por HTTP en vez de por consola. Elige las
 * implementaciones concretas igual que Main —es el otro extremo de la inyección por
 * constructor— y después no hace más que traducir: cada ruta arma una llamada a un
 * gestor y devuelve lo que responde.
 *
 * <p>Ninguna regla vive acá. Cuando un gestor rechaza algo lo hace con
 * IllegalArgumentException, y esta capa la convierte en un 400 con el mensaje intacto,
 * porque es el texto que el front le muestra al usuario.
 *
 * <p>MenuConsola sigue funcionando sin cambios: son dos interfaces sobre los mismos
 * gestores, y por eso esta clase tiene su propio main.
 */
public class ServidorApi {

    private static final Logger LOG = LoggerFactory.getLogger(ServidorApi.class);
    private static final int PUERTO_POR_DEFECTO = 8080;

    /** La forma de cualquier error, sea 400, 404 o 500. */
    record ErrorVista(String error) {
    }

    public static void main(String[] args) {
        PeliculaDAO peliculaDAO = new PeliculaDAOMySQL();
        SalaDAO salaDAO = new SalaDAOMySQL();
        FuncionDAO funcionDAO = new FuncionDAOMySQL();
        ClienteDAO clienteDAO = new ClienteDAOMySQL();
        AsientoDAO asientoDAO = new AsientoDAOMySQL();
        EmpleadoDAO empleadoDAO = new EmpleadoDAOMySQL();
        PagoDAO pagoDAO = new PagoDAOMySQL();
        ReservaDAO reservaDAO = new ReservaDAOMySQL();
        PromocionDAO promocionDAO = new PromocionDAOMySQL();
        // GestorClientes lo necesita para R12: no se borra un cliente con historial
        CompraCandyDAO compraCandyDAO = new CompraCandyDAOMySQL();
        GeneradorTicket generadorTicket = new GeneradorTicketTxt();

        GestorCartelera cartelera = new GestorCartelera(peliculaDAO, funcionDAO);
        GestorSalas salas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        GestorFunciones funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        GestorClientes clientes = new GestorClientes(clienteDAO, reservaDAO, compraCandyDAO);
        GestorEmpleados empleados = new GestorEmpleados(empleadoDAO);
        GestorPromociones promociones = new GestorPromociones(promocionDAO);
        GestorPagos pagos = new GestorPagos(pagoDAO, reservaDAO, funcionDAO, promociones);
        GestorReservas reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO,
                clienteDAO, peliculaDAO, generadorTicket);

        Vistas vistas = new Vistas(cartelera, salas, funciones, reservas, pagos, clientes);

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            // Un campo de más en el pedido no es motivo para rechazarlo: el front puede
            // mandar algo que este backend todavía no usa.
            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)));
        });

        RutasCatalogos.registrar(app);
        RutasPeliculas.registrar(app, cartelera, funciones, vistas);
        RutasSalas.registrar(app, salas, vistas);
        RutasFunciones.registrar(app, funciones, vistas);
        RutasClientes.registrar(app, clientes, vistas);
        RutasReservas.registrar(app, reservas, clientes, vistas);
        RutasPromociones.registrar(app, promociones, vistas);
        RutasPagos.registrar(app, pagos, reservas, vistas);
        RutasSesion.registrar(app, empleados, vistas);

        registrarErrores(app);
        app.start(puerto());
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
