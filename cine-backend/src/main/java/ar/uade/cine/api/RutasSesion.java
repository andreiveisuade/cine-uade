package ar.uade.cine.api;

import ar.uade.cine.servicio.GestorAdministradores;
import io.javalin.Javalin;

/**
 * Inicio de sesión del encargado (CU-10). El cliente no pasa por acá: compra sin
 * registrarse.
 *
 * <p>El gestor devuelve el mismo mensaje para email inexistente y contraseña
 * equivocada, así que esta capa no tiene que hacer nada para no filtrar qué emails
 * están registrados: le alcanza con no inventarle un mensaje propio.
 */
class RutasSesion {

    record PedidoSesion(String email, String password) {
    }

    static void registrar(Javalin app, GestorAdministradores administradores, Vistas vistas) {
        app.post("/api/sesion", ctx -> {
            PedidoSesion pedido = ctx.bodyAsClass(PedidoSesion.class);
            ctx.json(vistas.administrador(
                    administradores.iniciarSesion(pedido.email(), pedido.password())));
        });
    }
}
