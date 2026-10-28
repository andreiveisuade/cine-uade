package ar.uade.cine.controller.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.usuarios.PedidoCuentaDTO;
import ar.uade.cine.dto.usuarios.UsuarioVistaDTO;
import ar.uade.cine.service.usuarios.GestorClientes;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/cuenta, el cliente con la sesión de Google (R23 y R26); GestorClientes.
// El cliente es siempre el de la sesión: ninguna ruta recibe un email o un id de quien se modifica.
@Tag(name = "Cuenta", description = "Lo que el cliente hace con su cuenta")
@RestController
@RequiredArgsConstructor
public class CuentaController {

    private final GestorClientes clientes;
    private final VistasUsuarios vistas;

    @Operation(summary = "Cambiar el nombre del cliente de la sesión (R23)")
    @PutMapping("/api/cuenta")
    public UsuarioVistaDTO modificar(@Valid @RequestBody PedidoCuentaDTO pedido, Authentication identidad) {
        return vistas.usuario(clientes.renombrar(identidad.getName(), pedido.nombre()));
    }

    // R26: sin clave que borrar, la baja cierra la sesión; reservas, pagos y entradas son ventas del cine y quedan.
    // logout() corre los mismos handlers que DELETE /api/sesion: invalida la sesión y borra la cookie.
    @Operation(summary = "Dar de baja la cuenta: cierra la sesión; reservas y entradas quedan (R26)")
    @DeleteMapping("/api/cuenta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(HttpServletRequest http) throws ServletException {
        http.logout();
    }
}
