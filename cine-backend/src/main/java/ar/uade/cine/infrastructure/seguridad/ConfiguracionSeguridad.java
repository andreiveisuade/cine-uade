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

// HTTP Basic sin sesión: sin cookie no hay CSRF, por eso está apagado. Lo no enumerado pide ADMINISTRADOR.
// Los 401/403 salen sin WWW-Authenticate, que abriría el cuadro de login del navegador.
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private static final String ADMINISTRADOR = Rol.ADMINISTRADOR.name();
    private static final String ACOMODADOR = Rol.ACOMODADOR.name();

    // Públicas para que ConfiguracionOpenApi le saque el candado a las mismas rutas.

    public static final String[] POST_PUBLICOS = {
            "/api/sesion",
            "/api/clientes",
            "/api/reservas",
            "/api/funciones/*/bloqueos",
            "/api/reservas/codigo/*/cancelacion"};

    public static final String[] GET_PUBLICOS = {
            "/api/cartelera",
            "/api/peliculas/*",
            "/api/peliculas/*/funciones",
            "/api/funciones/*",
            "/api/reservas/codigo/*",
            "/api/candy/productos", "/api/candy/productos/*",
            "/api/generos", "/api/clasificaciones", "/api/tipos-sala",
            "/api/idiomas", "/api/proyecciones", "/api/medios-pago",
            "/api/tarifas"};

    public static final String[] GET_PROTEGIDOS_QUE_PARECEN_PUBLICOS = {"/api/peliculas/pendientes"};

    // Abierta solo con ?email=: ningún patrón de ruta mira el parámetro.
    public static final String GET_PUBLICO_CON_EMAIL = "/api/reservas";

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

                        .requestMatchers(HttpMethod.POST, POST_PUBLICOS).permitAll()
                        // Va antes que /api/peliculas/*, que si no lo abriría por coincidencia.
                        .requestMatchers(HttpMethod.GET, GET_PROTEGIDOS_QUE_PARECEN_PUBLICOS).hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET, GET_PUBLICOS).permitAll()
                        .requestMatchers(reservasDeUnEmail()).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/acceso").hasAnyRole(ADMINISTRADOR, ACOMODADOR)
                        .anyRequest().hasRole(ADMINISTRADOR))
                .build();
    }

    private static RequestMatcher reservasDeUnEmail() {
        return pedido -> HttpMethod.GET.matches(pedido.getMethod())
                && GET_PUBLICO_CON_EMAIL.equals(pedido.getServletPath())
                && pedido.getParameter("email") != null
                && !pedido.getParameter("email").isBlank();
    }

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
