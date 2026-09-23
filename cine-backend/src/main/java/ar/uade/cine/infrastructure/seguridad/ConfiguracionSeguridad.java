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
 * Quién puede llamar a qué. Antes no había nada: {@code POST /api/sesion} comprobaba la
 * clave y devolvía el empleado, pero ninguna otra ruta volvía a preguntar, así que
 * cualquiera con curl daba de alta una sala.
 *
 * <p><strong>HTTP Basic y sin sesión del lado del servidor.</strong> El front del encargado
 * manda {@code Authorization: Basic} en cada pedido y el backend no recuerda nada entre uno
 * y otro. Se eligió así porque es lo que menos mueve: no hay tokens que emitir, firmar ni
 * vencer, el login sigue siendo {@code POST /api/sesion} tal cual estaba, y los empleados y
 * sus hashes se leen de la tabla que ya existía. Sin sesión no hay cookie, y sin cookie
 * no hay CSRF que proteger: por eso está apagado, no por descuido. El costo es que la clave
 * viaja en cada pedido; detrás de HTTPS eso es aceptable, y en el TP corre en localhost.
 *
 * <p><strong>Tres niveles, y el cerrado es el default.</strong> Lo público es la lista
 * exacta de lo que usa el sitio del cliente —que compra sin registrarse— más el login y
 * Swagger; el acomodador suma la validación en la puerta (CU-18); todo lo demás pide
 * ADMINISTRADOR. Se enumera lo abierto y no lo cerrado para que una ruta nueva nazca
 * protegida: olvidarse de sumarla a la lista la deja sin usar desde el sitio público, que
 * se nota enseguida, y no expuesta, que no se nota nunca. Los GET no se abrieron en bloque
 * porque hay lecturas que son del encargado y llevan datos personales o de caja: la lista
 * completa de reservas con emails, el arqueo, el borderó, las compras del candy.
 *
 * <p><strong>Los 401 y 403 salen con la forma de siempre</strong>, {@code {"error": "..."}},
 * porque el front muestra ese campo y no tiene otro camino para los errores. Y salen
 * <em>sin</em> {@code WWW-Authenticate: Basic}: con esa cabecera el navegador abre su
 * propio cuadro de usuario y contraseña encima de la página, que es exactamente lo que el
 * login del panel viene a reemplazar.
 *
 * <p>Vive en {@code infrastructure/} porque es un adaptador más, del lado de la entrada: el
 * filtro corre antes que cualquier controlador y no sabe nada de las reglas del cine. Por
 * eso tampoco usa {@code ErrorVistaDTO} —{@code dto/} es de la capa de arriba— y escribe el
 * mismo JSON a mano.
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
                        // Clave equivocada y email inexistente dan lo mismo, con el mismo
                        // texto que el login: decir cuál falló confirma qué emails existen.
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
                // El entry point va también acá, no solo en exceptionHandling: es el que usa
                // el filtro de Basic cuando la clave no coincide, y el de fábrica es justo el
                // que agrega WWW-Authenticate.
                .httpBasic(basic -> basic.authenticationEntryPoint(sinIdentidad))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(sinIdentidad)
                        .accessDeniedHandler(sinPermiso))
                .authorizeHttpRequests(rutas -> rutas
                        // Un error que Spring reenvía a /error no es un pedido nuevo: si se
                        // lo volviera a autorizar, un 500 de una ruta pública saldría como 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // El login, y lo que hace el cliente para comprar sin registrarse.
                        .requestMatchers(HttpMethod.POST,
                                "/api/sesion",
                                "/api/clientes",
                                "/api/reservas",
                                "/api/funciones/*/bloqueos",
                                // "Mis reservas" deja cancelar la propia. Es tan
                                // abierto como consultarla: el cliente no tiene clave.
                                "/api/reservas/*/cancelacion").permitAll()

                        // Va antes que /api/peliculas/*, que si no lo abriría por coincidencia.
                        .requestMatchers(HttpMethod.GET, "/api/peliculas/pendientes").hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET,
                                "/api/cartelera",
                                "/api/peliculas/*",
                                "/api/peliculas/*/funciones",
                                "/api/funciones/*",
                                "/api/reservas/*",
                                // La carta: el ticket del cliente sugiere qué pedir en el candy.
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
     * {@code GET /api/reservas?email=...} es "mis reservas" del cliente; sin email es el
     * listado completo del encargado, con los datos de todos. Misma ruta, dos niveles: el
     * parámetro es lo único que los distingue, y ningún patrón de ruta lo mira.
     */
    private static RequestMatcher reservasDeUnEmail() {
        return pedido -> HttpMethod.GET.matches(pedido.getMethod())
                && "/api/reservas".equals(pedido.getServletPath())
                && pedido.getParameter("email") != null
                && !pedido.getParameter("email").isBlank();
    }

    /**
     * Los empleados, leídos de la misma tabla que usa el login. El rol de {@link Rol} pasa
     * a ser la autoridad {@code ROLE_ADMINISTRADOR} o {@code ROLE_ACOMODADOR}; un cliente no
     * aparece porque {@link EmpleadoRepository} solo trae empleados, que son los que tienen
     * contraseña.
     */
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
