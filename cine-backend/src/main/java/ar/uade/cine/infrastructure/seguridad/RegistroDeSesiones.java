package ar.uade.cine.infrastructure.seguridad;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

// Qué sesiones abiertas tiene cada cliente, por email, para cerrarlas al cambiar la clave; SessionRegistry.
// Spring lo usa con maximumSessions, que no aplica: el login es Basic y no pasa por SessionManagementFilter.
// Cerrar marca la sesión como vencida; FiltroDeSesionesCerradas la invalida en su próximo pedido.
@Component
public class RegistroDeSesiones {

    private final SessionRegistry sesiones;

    RegistroDeSesiones(SessionRegistry sesiones) {
        this.sesiones = sesiones;
    }

    void registrar(String idSesion, String email) {
        sesiones.registerNewSession(idSesion, email);
    }

    boolean cerrada(String idSesion) {
        SessionInformation sesion = sesiones.getSessionInformation(idSesion);
        return sesion != null && sesion.isExpired();
    }

    // R25, R26 y R27: la clave cambió sin la sesión del dueño (o la cuenta se cerró), ninguna sigue valiendo.
    public void cerrarTodas(String email) {
        cerrarLasDemas(email, null);
    }

    // R24: la del que cambió la clave sigue; las de otros dispositivos, con la clave vieja, no.
    public void cerrarLasDemas(String email, String idQueSigue) {
        sesiones.getAllSessions(email, false).stream()
                .filter(sesion -> !sesion.getSessionId().equals(idQueSigue))
                .forEach(SessionInformation::expireNow);
    }
}
