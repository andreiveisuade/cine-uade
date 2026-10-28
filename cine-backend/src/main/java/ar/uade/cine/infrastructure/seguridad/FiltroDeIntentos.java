package ar.uade.cine.infrastructure.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import ar.uade.cine.model.rechazos.DemasiadosIntentos;
import ar.uade.cine.model.usuarios.Email;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Antes del filtro Basic: con el email o la IP bloqueados contesta 429 sin llegar a bcrypt (~1 s en Render).
// Cuenta todo pedido con Basic, que es solo de empleados: el login y cada pedido de Swing. El 401 es clave equivocada.
// Sin @Component: como bean, Spring Boot además lo registraría como filtro suelto, fuera de la cadena.
class FiltroDeIntentos extends OncePerRequestFilter {

    private static final String PREFIJO_BASIC = "Basic ";

    private final LimiteDeIntentos limite;
    private final ObjectMapper json;

    FiltroDeIntentos(LimiteDeIntentos limite, ObjectMapper json) {
        this.limite = limite;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String email = emailDelBasic(pedido.getHeader(HttpHeaders.AUTHORIZATION));
        if (email == null) {
            cadena.doFilter(pedido, respuesta);
            return;
        }
        // Con forward-headers-strategy, getRemoteAddr ya es la IP del X-Forwarded-For del proxy de Render.
        String ip = pedido.getRemoteAddr();
        if (limite.bloqueado(email, ip)) {
            respuesta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(respuesta.getOutputStream(),
                    Map.of("error", DemasiadosIntentos.MENSAJE));
            return;
        }
        cadena.doFilter(pedido, respuesta);
        if (respuesta.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            limite.fallo(email, ip);
        } else {
            limite.exito(email);
        }
    }

    // Un Basic mal formado no se cuenta: el filtro Basic lo rechaza sin pasar por bcrypt.
    private static String emailDelBasic(String cabecera) {
        if (cabecera == null || !cabecera.startsWith(PREFIJO_BASIC)) {
            return null;
        }
        try {
            String credenciales = new String(Base64.getDecoder().decode(cabecera.substring(PREFIJO_BASIC.length())),
                    StandardCharsets.UTF_8);
            int separador = credenciales.indexOf(':');
            return separador < 0 ? null
                    : Email.paraBuscar(credenciales.substring(0, separador)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
