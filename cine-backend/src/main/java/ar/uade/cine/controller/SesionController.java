package ar.uade.cine.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.vistas.VistasUsuarios;
import ar.uade.cine.dto.usuarios.EmpleadoVistaDTO;
import ar.uade.cine.dto.usuarios.PedidoSesionDTO;
import ar.uade.cine.service.usuarios.GestorEmpleados;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Inicio de sesión del encargado (CU-10). No se inventa un mensaje propio: el del gestor es
 * el mismo para email inexistente y contraseña equivocada, y no filtra qué emails existen.
 */
@Tag(name = "Sesión", description = "El login del panel")
@RestController
public class SesionController {

    private final GestorEmpleados empleados;
    private final VistasUsuarios vistas;

    public SesionController(GestorEmpleados empleados, VistasUsuarios vistas) {
        this.empleados = empleados;
        this.vistas = vistas;
    }

    @Operation(summary = "Login del encargado. Es la única ruta que verifica credenciales")
    @PostMapping("/api/sesion")
    public EmpleadoVistaDTO iniciar(@RequestBody PedidoSesionDTO pedido) {
        return vistas.empleado(empleados.iniciarSesion(pedido.email(), pedido.password()));
    }
}
