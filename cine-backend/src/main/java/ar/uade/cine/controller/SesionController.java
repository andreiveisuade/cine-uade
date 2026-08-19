package ar.uade.cine.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.vistas.VistasUsuarios;
import ar.uade.cine.dto.usuarios.EmpleadoVistaDTO;
import ar.uade.cine.dto.usuarios.PedidoSesionDTO;
import ar.uade.cine.service.usuarios.GestorEmpleados;

/**
 * Inicio de sesión del encargado (CU-10). El cliente no pasa por acá: compra sin
 * registrarse.
 *
 * <p>El gestor devuelve el mismo mensaje para email inexistente y contraseña equivocada,
 * así que esta capa no tiene que hacer nada para no filtrar qué emails están registrados:
 * le alcanza con no inventarle un mensaje propio.
 */
@RestController
public class SesionController {

    private final GestorEmpleados empleados;
    private final VistasUsuarios vistas;

    public SesionController(GestorEmpleados empleados, VistasUsuarios vistas) {
        this.empleados = empleados;
        this.vistas = vistas;
    }

    @PostMapping("/api/sesion")
    public EmpleadoVistaDTO iniciar(@RequestBody PedidoSesionDTO pedido) {
        return vistas.empleado(empleados.iniciarSesion(pedido.email(), pedido.password()));
    }
}
