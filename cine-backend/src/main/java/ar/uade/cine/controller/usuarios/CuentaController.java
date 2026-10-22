package ar.uade.cine.controller.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.usuarios.ClaveTemporalVistaDTO;
import ar.uade.cine.dto.usuarios.PedidoBajaDTO;
import ar.uade.cine.dto.usuarios.PedidoCambioClaveDTO;
import ar.uade.cine.dto.usuarios.PedidoCuentaDTO;
import ar.uade.cine.dto.usuarios.PedidoRecuperacionDTO;
import ar.uade.cine.dto.usuarios.UsuarioVistaDTO;
import ar.uade.cine.service.usuarios.GestorCuentas;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/cuenta (el cliente con sesión, R23 a R26) y la clave temporal de boletería (R27); GestorCuentas.
// El cliente es siempre el de la sesión: ninguna ruta recibe un email o un id de quien se modifica.
@Tag(name = "Cuenta", description = "Lo que el cliente hace con su cuenta, y la clave temporal de boletería")
@RestController
@RequiredArgsConstructor
public class CuentaController {

    private final GestorCuentas cuentas;
    private final VistasUsuarios vistas;

    @Operation(summary = "Cambiar el nombre del cliente de la sesión (R23)")
    @PutMapping("/api/cuenta")
    public UsuarioVistaDTO modificar(@Valid @RequestBody PedidoCuentaDTO pedido, Authentication identidad) {
        return vistas.usuario(cuentas.renombrar(identidad.getName(), pedido.nombre()));
    }

    // Session fixation: el id cambia antes de cerrar las demás, y la respuesta lleva la cookie nueva.
    @Operation(summary = "Cambiar la clave con la actual; las otras sesiones del cliente se cierran (R24)")
    @PostMapping("/api/cuenta/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarClave(@Valid @RequestBody PedidoCambioClaveDTO pedido, Authentication identidad,
                             HttpServletRequest http) {
        cuentas.cambiarClave(identidad.getName(), pedido.actual(), pedido.nueva(), http.getRemoteAddr(),
                http::changeSessionId);
    }

    @Operation(summary = "Recuperar la clave sin sesión con el código de una entrada propia (R25)")
    @PostMapping("/api/cuenta/recuperacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recuperarClave(@Valid @RequestBody PedidoRecuperacionDTO pedido, HttpServletRequest http) {
        cuentas.recuperarClave(pedido.email(), pedido.codigoReserva(), pedido.nueva(), http.getRemoteAddr());
    }

    // logout() corre los mismos handlers que DELETE /api/sesion: invalida la sesión y borra la cookie.
    @Operation(summary = "Dar de baja la cuenta con la clave actual; reservas y entradas quedan (R26)")
    @DeleteMapping("/api/cuenta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@Valid @RequestBody PedidoBajaDTO pedido, Authentication identidad,
                          HttpServletRequest http) throws ServletException {
        cuentas.darDeBaja(identidad.getName(), pedido.actual(), http.getRemoteAddr());
        http.logout();
    }

    @Operation(summary = "Asignar una clave temporal a un cliente, desde boletería (R27)")
    @PostMapping("/api/clientes/{id}/clave-temporal")
    public ClaveTemporalVistaDTO asignarClaveTemporal(@PathVariable int id) {
        return new ClaveTemporalVistaDTO(cuentas.asignarClaveTemporal(id).valor());
    }
}
