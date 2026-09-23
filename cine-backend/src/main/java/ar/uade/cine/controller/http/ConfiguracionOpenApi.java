package ar.uade.cine.controller.http;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import ar.uade.cine.infrastructure.seguridad.ConfiguracionSeguridad;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class ConfiguracionOpenApi {

    private static final String ESQUEMA_ERROR = "Error";

    private static final String ESQUEMA_BASIC = "basic";

    private static final AntPathMatcher RUTAS = new AntPathMatcher();

    private static final Set<String> CON_NOMBRE_UNICO = Set.of(
            "POST /api/peliculas", "POST /api/peliculas/importadas", "PUT /api/peliculas/{id}",
            "POST /api/salas", "PUT /api/salas/{id}",
            "POST /api/clientes", "POST /api/promociones",
            "POST /api/candy/productos", "POST /api/candy/combos", "PUT /api/candy/productos/{id}");

    @Bean
    public OpenAPI apiDelCine() {
        return new OpenAPI()
                .info(new Info()
                        .title("API del cine")
                        .version("1.0")
                        .description("""
                                Cartelera, funciones, reserva de butacas, cobro, candy e informes. \
                                TPO de Aplicaciones Interactivas (UADE).

                                Las reglas de negocio (R1..R19) viven en la capa de servicio: esta API \
                                las expone, no las reimplementa.

                                **Autenticación: HTTP Basic** con el email y la contraseña de un \
                                empleado (botón *Authorize*). Sin credenciales se puede usar lo que \
                                usa el sitio del cliente —cartelera, detalle de película y función, \
                                catálogos, carta del candy, reservar, bloquear butacas, consultar y cancelar la propia \
                                reserva— y `POST /api/sesion`. `POST /api/acceso` pide ACOMODADOR o \
                                ADMINISTRADOR; todo lo demás, ADMINISTRADOR.
                                """))
                // Relativo: sirve igual detrás del nginx del compose que contra el backend directo.
                .servers(List.of(new Server().url("/").description("Este mismo servidor")));
    }

    // El esquema se registra acá y no en el bean OpenAPI porque springdoc pisa components.schemas después.
    @Bean
    public OpenApiCustomizer erroresYSeguridad() {
        return api -> {
            api.getComponents().addSchemas(ESQUEMA_ERROR, esquemaDeError());
            api.getComponents().addSecuritySchemes(ESQUEMA_BASIC, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP).scheme("basic")
                    .description("Email y contraseña de un empleado"));
            api.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BASIC));
            api.getPaths().forEach((ruta, item) -> item.readOperationsMap()
                    .forEach((metodo, operacion) -> documentar(ruta, metodo, operacion)));
        };
    }

    private static void documentar(String ruta, PathItem.HttpMethod metodo, Operation operacion) {
        ApiResponses respuestas = operacion.getResponses();
        boolean escribe = metodo != PathItem.HttpMethod.GET;
        boolean conParametros = operacion.getParameters() != null && operacion.getParameters().stream()
                .anyMatch(parametro -> "query".equals(parametro.getIn()));

        if (escribe || conParametros) {
            respuestas.addApiResponse("400", respuestaDeError("El pedido no es válido, o una regla de negocio lo rechazó"));
        }
        if (ruta.contains("{")) {
            respuestas.addApiResponse("404", respuestaDeError("No existe lo que se pidió"));
        }
        if (metodo == PathItem.HttpMethod.POST && ruta.equals("/api/reservas")) {
            respuestas.addApiResponse("409", respuestaDeError("La butaca ya estaba vendida: se perdió la carrera contra otra compra"));
        } else if (CON_NOMBRE_UNICO.contains(metodo + " " + ruta)) {
            respuestas.addApiResponse("409", respuestaDeError("Ya existe otro con ese nombre, email o título"));
        }
        if (ruta.equals("/api/sesion")) {
            respuestas.addApiResponse("401", respuestaDeError("Email o contraseña incorrectos"));
        }
        respuestas.addApiResponse("500", respuestaDeError("Falló el acceso a los datos o la emisión de un comprobante"));

        Acceso acceso = accesoDe(ruta, metodo);
        if (acceso == Acceso.PUBLICO) {
            operacion.setSecurity(List.of());
            return;
        }
        if (acceso == Acceso.PUBLICO_CON_EMAIL) {
            // Requisito vacío primero: con ?email= no hace falta credencial.
            operacion.setSecurity(List.of(new SecurityRequirement(),
                    new SecurityRequirement().addList(ESQUEMA_BASIC)));
        }
        respuestas.addApiResponse("401", respuestaDeError("Faltan las credenciales, o no corresponden a ningún empleado"));
        respuestas.addApiResponse("403", respuestaDeError("El rol de quien llama no alcanza para esta operación"));
    }

    private enum Acceso { PUBLICO, PUBLICO_CON_EMAIL, PROTEGIDO }

    private static Acceso accesoDe(String ruta, PathItem.HttpMethod metodo) {
        // /api/peliculas/{id} → /api/peliculas/x, para compararla con los patrones de seguridad.
        String concreta = ruta.replaceAll("\\{[^/]+}", "x");
        if (metodo == PathItem.HttpMethod.POST && coincide(concreta, ConfiguracionSeguridad.POST_PUBLICOS)) {
            return Acceso.PUBLICO;
        }
        if (metodo == PathItem.HttpMethod.GET) {
            if (coincide(ruta, ConfiguracionSeguridad.GET_PROTEGIDOS_QUE_PARECEN_PUBLICOS)) {
                return Acceso.PROTEGIDO;
            }
            if (coincide(concreta, ConfiguracionSeguridad.GET_PUBLICOS)) {
                return Acceso.PUBLICO;
            }
            if (ruta.equals(ConfiguracionSeguridad.GET_PUBLICO_CON_EMAIL)) {
                return Acceso.PUBLICO_CON_EMAIL;
            }
        }
        return Acceso.PROTEGIDO;
    }

    private static boolean coincide(String ruta, String[] patrones) {
        return Arrays.stream(patrones).anyMatch(patron -> RUTAS.match(patron, ruta));
    }

    private static ApiResponse respuestaDeError(String descripcion) {
        return new ApiResponse()
                .description(descripcion)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_ERROR))));
    }

    private static Schema<?> esquemaDeError() {
        return new Schema<>()
                .type("object")
                .description("La forma de cualquier error de la API")
                .addProperty("error", new StringSchema()
                        .description("El mensaje, tal como lo escribió el gestor que rechazó la operación")
                        .example("La butaca B4 ya está ocupada"));
    }
}
