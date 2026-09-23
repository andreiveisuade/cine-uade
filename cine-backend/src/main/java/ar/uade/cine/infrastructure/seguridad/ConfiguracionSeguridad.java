package ar.uade.cine.infrastructure.seguridad;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.EmpleadoRepository;
import ar.uade.cine.service.usuarios.CredencialesInvalidas;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Quién puede llamar a qué. HTTP Basic sin sesión: sin cookie no hay CSRF, por eso está
 * apagado. Se enumera lo abierto (sitio del cliente, login, Swagger; CU-18 para el
 * acomodador) y todo lo demás pide ADMINISTRADOR, así una ruta nueva nace protegida.
 * Los 401/403 salen como {@code {"error": "..."}} y sin {@code WWW-Authenticate}, que abriría
 * el cuadro de login del navegador. No usa {@code ErrorVistaDTO}: {@code dto/} está arriba.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private static final String ADMINISTRADOR = Rol.ADMINISTRADOR.name();
    private static final String ACOMODADOR = Rol.ACOMODADOR.name();

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http, ObjectMapper json) throws Exception {
        AuthenticationEntryPoint sinIdentidad = (pedido, respuesta, error) ->
                responder(respuesta, json, HttpStatus.UNAUTHORIZED,
                        // Mismo texto para clave y email: distinguirlos revela qué emails existen.
                        error instanceof BadCredentialsException
                                ? new CredencialesInvalidas().getMessage()
                                : "Hace falta iniciar sesión para esta operación");
        AccessDeniedHandler sinPermiso = (pedido, respuesta, error) ->
                responder(respuesta, json, HttpStatus.FORBIDDEN,
                        "Tu rol no tiene permiso para esta operación");

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // También acá: el de fábrica del filtro Basic agrega WWW-Authenticate.
                .httpBasic(basic -> basic.authenticationEntryPoint(sinIdentidad))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(sinIdentidad)
                        .accessDeniedHandler(sinPermiso))
                .authorizeHttpRequests(rutas -> rutas
                        // Reautorizar el reenvío a /error convertiría un 500 público en 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Login y compra sin registrarse.
                        .requestMatchers(HttpMethod.POST,
                                "/api/sesion",
                                "/api/clientes",
                                "/api/reservas",
                                "/api/funciones/*/bloqueos",
                                // Tan abierto como consultarla: el cliente no tiene clave.
                                "/api/reservas/*/cancelacion").permitAll()

                        // Va antes que /api/peliculas/*, que si no lo abriría por coincidencia.
                        .requestMatchers(HttpMethod.GET, "/api/peliculas/pendientes").hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET,
                                "/api/cartelera",
                                "/api/peliculas/*",
                                "/api/peliculas/*/funciones",
                                "/api/funciones/*",
                                "/api/reservas/*",
                                "/api/candy/productos", "/api/candy/productos/*",
                                "/api/generos", "/api/clasificaciones", "/api/tipos-sala",
                                "/api/idiomas", "/api/proyecciones", "/api/medios-pago",
                                "/api/tarifas").permitAll()
                        .requestMatchers(reservasDeUnEmail()).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/acceso").hasAnyRole(ADMINISTRADOR, ACOMODADOR)
                        .anyRequest().hasRole(ADMINISTRADOR))
                .build();
    }

    /**
     * Con email es "mis reservas" del cliente; sin email, el listado del encargado. Ningún
     * patrón de ruta mira el parámetro.
     */
    private static RequestMatcher reservasDeUnEmail() {
        return pedido -> HttpMethod.GET.matches(pedido.getMethod())
                && "/api/reservas".equals(pedido.getServletPath())
                && pedido.getParameter("email") != null
                && !pedido.getParameter("email").isBlank();
    }

    /** Solo empleados, que son los que tienen contraseña; el {@link Rol} pasa a ser {@code ROLE_*}. */
    @Bean
    public UserDetailsService empleadosComoUsuarios(EmpleadoRepository empleados) {
        return email -> empleados.findByEmail(email)
                .map(empleado -> User.withUsername(empleado.getEmail())
                        .password(empleado.getPasswordHash())
                        .roles(empleado.getRol().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No hay un empleado con ese email"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordSha256();
    }

    private static void responder(HttpServletResponse respuesta, ObjectMapper json,
                                  HttpStatus estado, String mensaje) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getOutputStream(), Map.of("error", mensaje));
    }
}
