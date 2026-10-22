package ar.uade.cine.infrastructure.seguridad;

import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import ar.uade.cine.model.usuarios.Rol;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Guarda en la sesión HTTP solo la identidad de un cliente; Decorator de HttpSessionSecurityContextRepository.
// Swing manda Basic en cada pedido: guardar ese login abriría una sesión que nadie usa.
class SesionDeClientes implements SecurityContextRepository {

    private static final String CLIENTE = "ROLE_" + Rol.CLIENTE.name();

    private final HttpSessionSecurityContextRepository sesiones = new HttpSessionSecurityContextRepository();
    private final RegistroDeSesiones registro;

    SesionDeClientes(RegistroDeSesiones registro) {
        this.registro = registro;
    }

    @Override
    public DeferredSecurityContext loadDeferredContext(HttpServletRequest pedido) {
        return sesiones.loadDeferredContext(pedido);
    }

    @Override
    @SuppressWarnings("deprecation")
    public SecurityContext loadContext(HttpRequestResponseHolder pedidoYRespuesta) {
        return sesiones.loadContext(pedidoYRespuesta);
    }

    // El id de sesión cambia al entrar: uno fijado de antemano por otro no queda autenticado (session fixation).
    @Override
    public void saveContext(SecurityContext contexto, HttpServletRequest pedido, HttpServletResponse respuesta) {
        if (!esCliente(contexto)) {
            return;
        }
        if (pedido.getSession(false) != null) {
            pedido.changeSessionId();
        }
        sesiones.saveContext(contexto, pedido, respuesta);
        registro.registrar(pedido.getSession().getId(), contexto.getAuthentication().getName());
    }

    @Override
    public boolean containsContext(HttpServletRequest pedido) {
        return sesiones.containsContext(pedido);
    }

    private static boolean esCliente(SecurityContext contexto) {
        return contexto.getAuthentication() != null && contexto.getAuthentication().getAuthorities().stream()
                .anyMatch(rol -> CLIENTE.equals(rol.getAuthority()));
    }
}
