package ar.uade.cine.infrastructure.seguridad;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.usuarios.EmpleadoRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

// Qué rol puede llamar cada ruta y cómo se autentica (Basic de empleados, Google del cliente); SecurityFilterChain.
// Empleados: Basic en cada pedido, sin sesión; cliente: Google una vez y cookie (SesionDeClientes). CSRF apagado: SameSite=Lax.
// 401/403 sin WWW-Authenticate, que abriría el login del navegador. Lo no enumerado pide ADMINISTRADOR.
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private static final String ADMINISTRADOR = Rol.ADMINISTRADOR.name();
    private static final String COOKIE_DE_SESION = "JSESSIONID";
    private static final String ACOMODADOR = Rol.ACOMODADOR.name();
    private static final String CLIENTE = Rol.CLIENTE.name();
    // La web (ruteo por hash) en el mismo origen: con nginx o servida por el backend en Render.
    private static final String VUELTA_A_LA_WEB = "/#/ingresar";
    private static final String NO_SE_PUDO = "No se pudo ingresar con Google: volvé a intentarlo";

    // Públicas para que ConfiguracionOpenApi le saque el candado a las mismas rutas.

    public static final String[] POST_PUBLICOS = {
            "/api/reservas",
            "/api/funciones/*/bloqueos"};

    public static final String[] GET_PUBLICOS = {
            "/api/cartelera",
            "/api/sesion/google",
            "/api/peliculas/*",
            "/api/peliculas/*/funciones",
            "/api/funciones/*",
            "/api/reservas/codigo/*",
            "/api/checkouts/*/retorno",
            "/api/candy/productos", "/api/candy/productos/*",
            "/api/imagenes/*",
            "/api/promociones/vigentes",
            "/api/generos", "/api/clasificaciones", "/api/tipos-sala",
            "/api/idiomas", "/api/proyecciones", "/api/medios-pago",
            "/api/tarifas", "/api/tipos-producto", "/api/tipos-promocion"};

    // El build de React cuando el backend lo sirve (Render); con nginx no llegan acá.
    private static final String[] FRONT = {
            "/", "/index.html", "/privacidad.html", "/assets/**",
            "/favicon.svg", "/favicon.ico", "/icon-192.png", "/icon-512.png",
            "/apple-touch-icon.png", "/og.jpg", "/site.webmanifest"};

    public static final String[] GET_PROTEGIDOS_QUE_PARECEN_PUBLICOS = {"/api/peliculas/pendientes"};

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http, ObjectMapper json, LimiteDeIntentos intentos,
                                               PropiedadesGoogle google, ClientesDeGoogle clientesDeGoogle)
            throws Exception {
        SecurityContextRepository sesion = new SesionDeClientes();
        AuthenticationEntryPoint sinIdentidad = (pedido, respuesta, error) ->
                responder(respuesta, json, HttpStatus.UNAUTHORIZED,
                        // Mismo texto para clave y email: distinguirlos revela qué emails existen.
                        error instanceof BadCredentialsException
                                ? "Email o contraseña incorrectos"
                                : "Hace falta iniciar sesión para esta operación");
        AccessDeniedHandler sinPermiso = (pedido, respuesta, error) ->
                responder(respuesta, json, HttpStatus.FORBIDDEN,
                        "Tu rol no tiene permiso para esta operación");

        // R28: sin credenciales no hay login con Google; /oauth2/** cae en el anyRequest y da 401.
        if (google.configurado()) {
            http.oauth2Login(login -> login
                    .clientRegistrationRepository(new InMemoryClientRegistrationRepository(
                            CommonOAuth2Provider.GOOGLE.getBuilder("google")
                                    .clientId(google.clientId())
                                    .clientSecret(google.clientSecret())
                                    .scope("openid", "email", "profile")
                                    .build()))
                    // En la sesión y no en un mapa en memoria que crecería con cada cliente que entró alguna vez.
                    .authorizedClientRepository(new HttpSessionOAuth2AuthorizedClientRepository())
                    // Sin la página de login que Spring genera en /login: el ingreso es #/ingresar.
                    .loginPage(VUELTA_A_LA_WEB)
                    .userInfoEndpoint(usuario -> usuario.oidcUserService(clientesDeGoogle))
                    .successHandler(new SimpleUrlAuthenticationSuccessHandler(VUELTA_A_LA_WEB))
                    .failureHandler(vueltaConError()));
        }
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(salida -> salida
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults()
                                .matcher(HttpMethod.DELETE, "/api/sesion"))
                        .deleteCookies(COOKIE_DE_SESION)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .securityContext(contexto -> contexto.securityContextRepository(sesion))
                .addFilterBefore(new FiltroDeIntentos(intentos, json), BasicAuthenticationFilter.class)
                // Guardaría en la sesión cada pedido que da 401, y crearía una para cualquiera.
                .requestCache(AbstractHttpConfigurer::disable)
                // También acá: el de fábrica del filtro Basic agrega WWW-Authenticate.
                .httpBasic(basic -> basic.authenticationEntryPoint(sinIdentidad).securityContextRepository(sesion))
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
                        .requestMatchers(HttpMethod.GET, FRONT).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/sesion").hasAnyRole(ADMINISTRADOR, ACOMODADOR, CLIENTE)
                        // R21: el dueño cancela y paga online la suya; que sea suya lo decide GestorReservas.
                        .requestMatchers(HttpMethod.GET, "/api/reservas/mias").hasRole(CLIENTE)
                        .requestMatchers(HttpMethod.PUT, "/api/cuenta").hasRole(CLIENTE)
                        .requestMatchers(HttpMethod.DELETE, "/api/cuenta").hasRole(CLIENTE)
                        .requestMatchers(HttpMethod.POST, "/api/reservas/codigo/*/cancelacion",
                                "/api/reservas/codigo/*/checkout").hasRole(CLIENTE)
                        // El acomodador entra al panel para llegar a la puerta.
                        .requestMatchers(HttpMethod.POST, "/api/acceso").hasAnyRole(ADMINISTRADOR, ACOMODADOR)
                        .anyRequest().hasRole(ADMINISTRADOR))
                .build();
    }

    // El motivo viaja en la URL de la web, que lo muestra: solo los rechazos del cine, el resto con un texto genérico.
    private static AuthenticationFailureHandler vueltaConError() {
        return (pedido, respuesta, error) -> {
            String mensaje = error instanceof OAuth2AuthenticationException e
                    && ClientesDeGoogle.RECHAZO.equals(e.getError().getErrorCode()) ? e.getMessage() : NO_SE_PUDO;
            respuesta.sendRedirect(VUELTA_A_LA_WEB + "?error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
        };
    }

    // Lax y no Strict: con Strict, entrar al cine desde un link de otro sitio llegaría sin la sesión.
    @Bean
    public CookieSameSiteSupplier cookieDeSesionSoloDelMismoSitio() {
        return CookieSameSiteSupplier.ofLax().whenHasName(COOKIE_DE_SESION);
    }

    // El email se busca como lo guarda Email: el login tampoco distingue mayúsculas ni espacios.
    // Solo empleados: el cliente entra con Google (R28). Sin hash cuenta como email inexistente; User con NULL tiraba.
    @Bean
    public UserDetailsService usuariosConClave(EmpleadoRepository empleados) {
        return email -> Email.paraBuscar(email)
                .flatMap(empleados::findByEmail)
                .filter(empleado -> empleado.getPasswordHash() != null)
                .map(empleado -> User.withUsername(empleado.getEmail())
                        .password(empleado.getPasswordHash())
                        .roles(empleado.getRol().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No hay un usuario con ese email"));
    }

    // Los hashes guardados llevan el prefijo {bcrypt}; solo bcrypt en el mapa: el de fábrica acepta {noop}.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder()));
    }

    // Sin charset, como ManejadorErrores: JSON es UTF-8 por definición
    private static void responder(HttpServletResponse respuesta, ObjectMapper json,
                                  HttpStatus estado, String mensaje) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(respuesta.getOutputStream(), Map.of("error", mensaje));
    }
}
