package ar.uade.cine.api.rutas;

import ar.uade.cine.Aplicacion;
import ar.uade.cine.api.vistas.Vistas;
import io.javalin.Javalin;

/**
 * Qué gestor atiende cada familia de rutas, y con qué vistas arma su respuesta.
 *
 * <p>Es la <strong>única</strong> clase pública de este paquete, a propósito. Las
 * {@code Rutas*} son package-private y solo se las puede registrar desde acá: nadie de
 * afuera puede colgar media API del servidor, ni registrar una familia suelta y olvidarse
 * de las otras. Es el mismo criterio con el que {@link Aplicacion} es el único lugar donde
 * se arma el sistema.
 *
 * <p>Cada grupo recibe solo la vista que usa. Antes había una sola clase Vistas que las
 * armaba todas y por eso necesitaba seis gestores: la ruta de sesión cargaba con el gestor
 * de pagos para devolver un empleado.
 */
public final class Rutas {

    private Rutas() {
    }

    public static void registrarTodas(Javalin app, Aplicacion aplicacion) {
        Vistas vistas = Vistas.de(aplicacion);

        RutasCatalogos.registrar(app);
        RutasPeliculas.registrar(app, aplicacion.getCartelera(),
                aplicacion.getRevisionCartelera(), aplicacion.getFunciones(),
                vistas.cartelera());
        // Las importaciones no necesitan vistas: una corrida se dibuja sola.
        RutasImportaciones.registrar(app, aplicacion.getImportaciones());
        RutasSalas.registrar(app, aplicacion.getSalas(), vistas.salas());
        RutasFunciones.registrar(app, aplicacion.getFunciones(), vistas.cartelera());
        // La grilla arma sus propias vistas adentro de la ruta: no necesita ningun gestor
        // mas que el suyo para dibujarse.
        RutasProgramaciones.registrar(app, aplicacion.getProgramaciones());
        RutasGrilla.registrar(app, aplicacion.getPlanificadorGrilla());
        RutasClientes.registrar(app, aplicacion.getClientes(), vistas.usuarios());
        RutasReservas.registrar(app, aplicacion.getReservas(), aplicacion.getClientes(),
                aplicacion.getOcupacion(), vistas.ventas());
        RutasPromociones.registrar(app, aplicacion.getPromociones(), vistas.promociones());
        RutasPagos.registrar(app, aplicacion.getPagos(), aplicacion.getReservas(),
                aplicacion.getCaja(), vistas.ventas());
        // Los informes por función arman sus DTO adentro de la ruta: ya vienen calculados
        // del gestor y no necesitan ensamblador.
        RutasInformes.registrar(app, aplicacion.getInformes(), aplicacion.getFunciones());
        RutasCandy.registrar(app, aplicacion.getCandy(), aplicacion.getProductos(),
                aplicacion.getCaja(), vistas.candy());
        RutasSesion.registrar(app, aplicacion.getEmpleados(), vistas.usuarios());
    }
}
