package ar.uade.cine.api;

import ar.uade.cine.dto.usuarios.PedidoSesionDTO;
import ar.uade.cine.servicio.GestorEmpleados;
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

    static void registrar(Javalin app, GestorEmpleados empleados, VistasUsuarios vistas) {
        app.post("/api/sesion", ctx -> {
            PedidoSesionDTO pedido = ctx.bodyAsClass(PedidoSesionDTO.class);
            ctx.json(vistas.empleado(
                    empleados.iniciarSesion(pedido.email(), pedido.password())));
        });
    }
}
