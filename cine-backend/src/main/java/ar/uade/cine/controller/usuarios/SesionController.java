package ar.uade.cine.controller.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.usuarios.IngresoConGoogleVistaDTO;
import ar.uade.cine.dto.usuarios.UsuarioVistaDTO;
import ar.uade.cine.infrastructure.seguridad.PropiedadesGoogle;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.usuarios.GestorEmpleados;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Ruta POST /api/sesion: quién entró, el empleado con Basic o el cliente con la cookie de Google (R28).
// El cierre, DELETE /api/sesion, no tiene controller: lo atiende el filtro de logout.
@Tag(name = "Sesión", description = "El login del panel y la sesión del cliente")
@RestController
@RequiredArgsConstructor
public class SesionController {

    private final GestorEmpleados empleados;
    private final GestorClientes clientes;
    private final VistasUsuarios vistas;
    private final PropiedadesGoogle google;

    // El filtro Basic verifica las credenciales del empleado (un solo camino); el cuerpo se ignora.
    @Operation(summary = "Login del empleado con HTTP Basic, o el cliente de la sesión de Google: devuelve quién es")
    @PostMapping("/api/sesion")
    public UsuarioVistaDTO iniciar(Authentication identidad) {
        String email = identidad.getName();
        return empleados.buscarPorEmail(email).map(vistas::usuario)
                .or(() -> clientes.buscarPorEmail(email).map(vistas::usuario))
                .orElseThrow();
    }

    @Operation(summary = "Si el cliente puede ingresar con Google: sin credenciales de Google Cloud, no")
    @GetMapping("/api/sesion/google")
    public IngresoConGoogleVistaDTO ingresoConGoogle() {
        return new IngresoConGoogleVistaDTO(google.configurado());
    }
}
