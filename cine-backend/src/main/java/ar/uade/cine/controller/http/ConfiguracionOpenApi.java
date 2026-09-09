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
import io.swagger.v3.oas.models.servers.Server;

/**
 * La portada de la API y el contrato de errores, en un solo lugar.
 *
 * <p>La lista de endpoints no se escribe acá: springdoc la deduce leyendo los
 * {@code @RestController} y los tipos de sus DTO. Eso es justamente lo que se busca —una
 * doc que no se puede desactualizar, porque sale del mismo código que atiende el pedido—.
 * Lo que sí hay que decirle es lo que no está en ninguna firma: cómo se llama el sistema y
 * qué le puede pasar a cualquier ruta.
 *
 * <p>Vive en {@code controller/http/} y no en {@code infrastructure/} por la misma razón que
 * {@link ManejadorErrores}: describe la superficie HTTP, que es de lo que se ocupa esta capa.
 */
@Configuration
public class ConfiguracionOpenApi {

    /** El nombre del esquema compartido de error. Uno solo, igual que {@code ErrorVistaDTO}. */
    private static final String ESQUEMA_ERROR = "Error";

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

                                **La API todavía no valida quién llama.** Solo `POST /api/sesion` \
                                verifica credenciales; el resto de las rutas de escritura no comprueba \
                                que haya sesión iniciada. Está anotado como pendiente.
                                """))
                // Relativo: sirve igual detrás del nginx del compose que contra el backend directo.
                .servers(List.of(new Server().url("/").description("Este mismo servidor")));
    }

    /**
     * Suma los errores comunes a todas las operaciones.
     *
     * <p>Los cuatro salen de {@link ManejadorErrores}, que los aplica globalmente con un
     * {@code @RestControllerAdvice}: cualquier ruta puede devolverlos aunque su método no
     * los mencione. Anotarlos endpoint por endpoint serían 68 repeticiones que se
     * desincronizan a la primera excepción nueva; acá se declaran una vez y salen en todas.
     *
     * <p>El esquema compartido también se registra acá y no en el bean {@code OpenAPI}:
     * springdoc arma {@code components.schemas} desde los tipos que encuentra en los
     * controladores y <strong>reemplaza</strong> lo que hubiera puesto el bean. Declarado
     * allá, los {@code $ref} de abajo quedaban apuntando a un esquema inexistente. El
     * customizer corre después de esa detección, así que acá el agregado sobrevive.
     * {@code ErrorVistaDTO} no aparece solo porque ningún método lo declara como retorno:
     * lo devuelve el advice, que springdoc no inspecciona.
     */
    @Bean
    public OpenApiCustomizer erroresComunes() {
        return api -> {
            api.getComponents().addSchemas(ESQUEMA_ERROR, esquemaDeError());
            api.getPaths().values().stream()
                    .flatMap(ruta -> ruta.readOperations().stream())
                    .forEach(operacion -> {
                        ApiResponses respuestas = operacion.getResponses();
                        Map.of(
                                "400", "El pedido no es válido, o una regla de negocio lo rechazó",
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
