package ar.uade.cine.controller.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.usuarios.UsuarioVistaDTO;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.usuarios.GestorEmpleados;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Ruta POST /api/sesion (login): devuelve el usuario ya autenticado; la clave la valida Spring Security.
// El cierre, DELETE /api/sesion, no tiene controller: lo atiende el filtro de logout.
@Tag(name = "Sesión", description = "El login del panel y del cliente con cuenta")
@RestController
@RequiredArgsConstructor
public class SesionController {

    private final GestorEmpleados empleados;
    private final GestorClientes clientes;
    private final VistasUsuarios vistas;

    // Las credenciales las verifica el filtro Basic, igual que en cualquier otra ruta: si el
    // login las comparara por su cuenta, habría dos caminos que pueden no coincidir. El cuerpo
    // no se lee, así que un cliente viejo que todavía manda {email,password} sigue entrando.
    // Si entra un cliente, el filtro ya le abrió la sesión y la respuesta lleva la cookie.
    @Operation(summary = "Login con HTTP Basic: devuelve quién se identificó; al cliente le abre la sesión con cookie")
    @PostMapping("/api/sesion")
    public UsuarioVistaDTO iniciar(Authentication identidad) {
        String email = identidad.getName();
        return empleados.buscarPorEmail(email).map(vistas::usuario)
                .or(() -> clientes.buscarPorEmail(email).map(vistas::usuario))
                .orElseThrow();
    }
}
