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
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.usuarios.EmpleadoRepository;
import ar.uade.cine.service.usuarios.GestorEmpleados;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

// Qué rol puede llamar cada ruta y cómo se autentica (Basic contra empleado, bcrypt); SecurityFilterChain.
// HTTP Basic sin sesión: sin cookie no hay CSRF, por eso está apagado. Lo no enumerado pide ADMINISTRADOR.
// Los 401/403 salen sin WWW-Authenticate, que abriría el cuadro de login del navegador.
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private static final String ADMINISTRADOR = Rol.ADMINISTRADOR.name();
    private static final String ACOMODADOR = Rol.ACOMODADOR.name();

    // Públicas para que ConfiguracionOpenApi le saque el candado a las mismas rutas.

    public static final String[] POST_PUBLICOS = {
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
            "/api/tarifas", "/api/tipos-producto", "/api/tipos-promocion"};

    public static final String[] GET_PROTEGIDOS_QUE_PARECEN_PUBLICOS = {"/api/peliculas/pendientes"};

    // Abierta solo con ?email=: ningún patrón de ruta mira el parámetro.
    public static final String GET_PUBLICO_CON_EMAIL = "/api/reservas";

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http, ObjectMapper json) throws Exception {
        AuthenticationEntryPoint sinIdentidad = (pedido, respuesta, error) ->
                responder(respuesta, json, HttpStatus.UNAUTHORIZED,
                        // Mismo texto para clave y email: distinguirlos revela qué emails existen.
                        error instanceof BadCredentialsException
                                ? "Email o contraseña incorrectos"
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

                        // El login también: el acomodador entra al panel para llegar a la puerta.
                        .requestMatchers(HttpMethod.POST, "/api/sesion", "/api/acceso")
                        .hasAnyRole(ADMINISTRADOR, ACOMODADOR)
                        .anyRequest().hasRole(ADMINISTRADOR))
                .build();
    }

    private static RequestMatcher reservasDeUnEmail() {
        return pedido -> HttpMethod.GET.matches(pedido.getMethod())
                && GET_PUBLICO_CON_EMAIL.equals(pedido.getServletPath())
                && pedido.getParameter("email") != null
                && !pedido.getParameter("email").isBlank();
    }

    // El email se busca como lo guarda Email: el login tampoco distingue mayúsculas ni espacios.
    // Un empleado sin hash (una fila cargada a mano con NULL) no tiene con qué comparar la clave: cuenta
    // como un email que no existe, así el 401 dice lo mismo que una clave equivocada. Armar el User con
    // NULL tiraba, y el 401 decía «Hace falta iniciar sesión».
    @Bean
    public UserDetailsService empleadosComoUsuarios(EmpleadoRepository empleados) {
        return email -> Email.paraBuscar(email).map(Email::valor).flatMap(empleados::findByEmail)
                .filter(empleado -> empleado.getPasswordHash() != null)
                .map(empleado -> User.withUsername(empleado.getEmail())
                        .password(empleado.getPasswordHash())
                        .roles(empleado.getRol().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No hay un empleado con ese email"));
    }

    // Lo nuevo sale como {bcrypt}…; un hash sin prefijo es el SHA-256 de antes y se sigue
    // aceptando, para no romper el seed ni las bases ya creadas. Solo bcrypt en el mapa: el
    // de fábrica de Spring también aceptaría {noop}, una clave en texto plano en la base.
    @Bean
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder claves = new DelegatingPasswordEncoder("bcrypt",
                Map.of("bcrypt", new BCryptPasswordEncoder()));
        claves.setDefaultPasswordEncoderForMatches(new PasswordSha256());
        return claves;
    }

    // Spring Security lo llama después de un login correcto cuyo hash no es bcrypt: es el único
    // momento en que se tiene la clave en claro. Sin esto los SHA-256 no migrarían nunca, porque
    // no hay pantalla para cambiar la contraseña.
    @Bean
    public UserDetailsPasswordService rehashearAlEntrar(GestorEmpleados empleados) {
        return (usuario, hashNuevo) -> {
            empleados.reemplazarHash(usuario.getUsername(), hashNuevo);
            return User.withUserDetails(usuario).password(hashNuevo).build();
        };
    }

    // Sin charset, igual que los errores de ManejadorErrores: Jackson escribe los bytes en UTF-8 de
    // todos modos, y JSON es UTF-8 por definición.
    private static void responder(HttpServletResponse respuesta, ObjectMapper json,
                                  HttpStatus estado, String mensaje) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(respuesta.getOutputStream(), Map.of("error", mensaje));
    }
}
