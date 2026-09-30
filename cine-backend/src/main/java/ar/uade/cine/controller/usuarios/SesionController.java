package ar.uade.cine.controller.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.usuarios.EmpleadoVistaDTO;
import ar.uade.cine.service.usuarios.GestorEmpleados;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Ruta POST /api/sesion (login): devuelve el empleado ya autenticado; la clave la valida Spring Security.
@Tag(name = "Sesión", description = "El login del panel")
@RestController
@RequiredArgsConstructor
public class SesionController {

    private final GestorEmpleados empleados;
    private final VistasUsuarios vistas;

    // Las credenciales las verifica el filtro Basic, igual que en cualquier otra ruta: si el
    // login las comparara por su cuenta, habría dos caminos que pueden no coincidir. El cuerpo
    // no se lee, así que un cliente viejo que todavía manda {email,password} sigue entrando.
    @Operation(summary = "Login del panel: con HTTP Basic, devuelve el empleado que se identificó")
    @PostMapping("/api/sesion")
    public EmpleadoVistaDTO iniciar(Authentication identidad) {
        return vistas.empleado(empleados.buscarPorEmail(identidad.getName()).orElseThrow());
    }
}
