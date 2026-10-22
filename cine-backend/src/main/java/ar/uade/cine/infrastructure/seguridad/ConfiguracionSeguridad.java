package ar.uade.cine.infrastructure.seguridad;

import java.io.IOException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.model.usuarios.Usuario;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.usuarios.EmpleadoRepository;
import ar.uade.cine.service.usuarios.GestorEmpleados;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

// Qué rol puede llamar cada ruta y cómo se autentica (Basic contra usuario, bcrypt); SecurityFilterChain.
// Empleados: Basic en cada pedido, sin sesión; cliente: Basic una vez y cookie (SesionDeClientes). CSRF apagado: SameSite=Lax.
// 401/403 sin WWW-Authenticate, que abriría el login del navegador. Lo no enumerado pide ADMINISTRADOR.
@Slf4j
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private static final String ADMINISTRADOR = Rol.ADMINISTRADOR.name();
    private static final String COOKIE_DE_SESION = "JSESSIONID";
    private static final String ACOMODADOR = Rol.ACOMODADOR.name();
    private static final String CLIENTE = Rol.CLIENTE.name();

    // Públicas para que ConfiguracionOpenApi le saque el candado a las mismas rutas.

    public static final String[] POST_PUBLICOS = {
            "/api/clientes",
            "/api/reservas",
            "/api/funciones/*/bloqueos"};

    public static final String[] GET_PUBLICOS = {
            "/api/cartelera",
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
            "/", "/index.html", "/assets/**",
            "/favicon.svg", "/favicon.ico", "/icon-192.png", "/icon-512.png",
            "/apple-touch-icon.png", "/og.jpg", "/site.webmanifest"};

    public static final String[] GET_PROTEGIDOS_QUE_PARECEN_PUBLICOS = {"/api/peliculas/pendientes"};

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http, ObjectMapper json) throws Exception {
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

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(salida -> salida
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults()
                                .matcher(HttpMethod.DELETE, "/api/sesion"))
                        .deleteCookies(COOKIE_DE_SESION)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .securityContext(contexto -> contexto.securityContextRepository(sesion))
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
                        .requestMatchers(HttpMethod.POST, "/api/reservas/codigo/*/cancelacion",
                                "/api/reservas/codigo/*/checkout").hasRole(CLIENTE)
                        // El acomodador entra al panel para llegar a la puerta.
                        .requestMatchers(HttpMethod.POST, "/api/acceso").hasAnyRole(ADMINISTRADOR, ACOMODADOR)
                        .anyRequest().hasRole(ADMINISTRADOR))
                .build();
    }

    // Lax y no Strict: con Strict, entrar al cine desde un link de otro sitio llegaría sin la sesión.
    @Bean
    public CookieSameSiteSupplier cookieDeSesionSoloDelMismoSitio() {
        return CookieSameSiteSupplier.ofLax().whenHasName(COOKIE_DE_SESION);
    }

    // El email se busca como lo guarda Email: el login tampoco distingue mayúsculas ni espacios.
    // Sin hash (cliente sin registrar o empleado con NULL) cuenta como email inexistente; User con NULL tiraba.
    @Bean
    public UserDetailsService usuariosConClave(EmpleadoRepository empleados, ClienteRepository clientes) {
        return email -> Email.paraBuscar(email).map(Email::valor)
                .<Usuario>flatMap(valor -> empleados.findByEmail(valor).<Usuario>map(empleado -> empleado)
                        .or(() -> clientes.findByEmail(valor)))
                .filter(usuario -> usuario.getPasswordHash() != null)
                .map(usuario -> User.withUsername(usuario.getEmail())
                        .password(usuario.getPasswordHash())
                        .roles(usuario.getRol().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No hay un usuario con ese email"));
    }

    // Hash sin prefijo = SHA-256 viejo (seed y bases previas); solo bcrypt en el mapa: el de fábrica acepta {noop}.
    @Bean
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder claves = new DelegatingPasswordEncoder("bcrypt",
                Map.of("bcrypt", new BCryptPasswordEncoder()));
        claves.setDefaultPasswordEncoderForMatches(new PasswordSha256());
        return new PasswordBcrypt(claves);
    }

    // Único momento con la clave en claro: sin este re-hash los SHA-256 no migran nunca (no hay cambio de clave).
    // Si el hash nuevo es el mismo (clave fuera de bcrypt, ver PasswordBcrypt) no se guarda y queda en el log.
    @Bean
    public UserDetailsPasswordService rehashearAlEntrar(GestorEmpleados empleados) {
        return (usuario, hashNuevo) -> {
            if (hashNuevo.equals(usuario.getPassword())) {
                log.warn("{} no pasa a bcrypt: su clave tiene más de 72 bytes y el hash sigue en SHA-256",
                        usuario.getUsername());
                return usuario;
            }
            empleados.reemplazarHash(usuario.getUsername(), hashNuevo);
            return User.withUserDetails(usuario).password(hashNuevo).build();
        };
    }

    // Sin charset, como ManejadorErrores: JSON es UTF-8 por definición
    private static void responder(HttpServletResponse respuesta, ObjectMapper json,
                                  HttpStatus estado, String mensaje) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(respuesta.getOutputStream(), Map.of("error", mensaje));
    }
}
