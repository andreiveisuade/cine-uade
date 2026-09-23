package ar.uade.cine.controller.http;

import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
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

/**
 * Lo que springdoc no deduce de los controladores: la portada de la API y los errores que
 * puede devolver cualquier ruta.
 */
@Configuration
public class ConfiguracionOpenApi {

    private static final String ESQUEMA_ERROR = "Error";

    private static final String ESQUEMA_BASIC = "basic";

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

    /**
     * Los errores comunes, declarados una vez en vez de en cada operación: los de negocio los
     * aplica {@link ManejadorErrores} a todas las rutas, y el 401/403 el filtro de seguridad.
     * El esquema se registra acá y no en el bean {@code OpenAPI} porque springdoc pisa
     * {@code components.schemas} después de armar el bean.
     */
    @Bean
    public OpenApiCustomizer erroresComunes() {
        return api -> {
            api.getComponents().addSchemas(ESQUEMA_ERROR, esquemaDeError());
            // Global: marcar ruta por ruta repetiría la lista de ConfiguracionSeguridad.
            api.getComponents().addSecuritySchemes(ESQUEMA_BASIC, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP).scheme("basic")
                    .description("Email y contraseña de un empleado"));
            api.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BASIC));
            api.getPaths().values().stream()
                    .flatMap(ruta -> ruta.readOperations().stream())
                    .forEach(operacion -> {
                        ApiResponses respuestas = operacion.getResponses();
                        Map.of(
                                "400", "El pedido no es válido, o una regla de negocio lo rechazó",
                                "401", "Faltan las credenciales, o no corresponden a ningún empleado",
                                "403", "El rol de quien llama no alcanza para esta operación",
                                "404", "No existe lo que se pidió",
                                "409", "La butaca ya estaba vendida: se perdió la carrera contra otra compra",
                                "500", "Falló el acceso a los datos o la emisión de un comprobante")
                                .forEach((codigo, descripcion) -> respuestas.addApiResponse(
                                        codigo, respuestaDeError(descripcion)));
                    });
        };
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
