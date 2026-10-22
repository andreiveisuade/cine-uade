package ar.uade.cine.infrastructure.seguridad;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Invalida la sesión que RegistroDeSesiones cerró y sigue como si no hubiera cookie; filtro antes del Basic.
// No es el ConcurrentSessionFilter de Spring: ese corta el pedido, y un login con la cookie vieja daría 401.
// Sin @Component: como bean, Spring Boot además lo registraría como filtro suelto, fuera de la cadena.
class FiltroDeSesionesCerradas extends OncePerRequestFilter {

    private final RegistroDeSesiones registro;

    FiltroDeSesionesCerradas(RegistroDeSesiones registro) {
        this.registro = registro;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        HttpSession sesion = pedido.getSession(false);
        if (sesion != null && registro.cerrada(sesion.getId())) {
            sesion.invalidate();
        }
        cadena.doFilter(pedido, respuesta);
    }
}
