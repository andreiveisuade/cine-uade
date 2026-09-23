package ar.uade.cine.controller.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.uade.cine.PruebaDeApi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Que el contrato de Swagger sea un documento íntegro: springdoc reemplaza los
 * {@code components.schemas} del bean {@code OpenAPI}, y los {@code $ref} a errores pueden
 * quedar apuntando a la nada sin que nada falle. Qué endpoints hay lo prueban los
 * {@code *ControllerTest}.
 */
class ContratoOpenApiTest extends PruebaDeApi {

    @Test
    @DisplayName("el contrato se publica y se identifica")
    void elContratoSePublica() {
        Respuesta respuesta = get("/v3/api-docs");

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.json().get("info").get("title").asText()).isEqualTo("API del cine");
    }

    @Test
    @DisplayName("todas las rutas de la API entran en el contrato")
    void todasLasRutasEntranEnElContrato() {
        JsonNode rutas = get("/v3/api-docs").json().get("paths");

        // Sin número fijo: se afirma que springdoc encontró los controladores; un contrato vacío
        // también devolvería 200.
        assertThat(rutas).isNotEmpty();
        rutas.properties().forEach(ruta -> assertThat(ruta.getKey()).startsWith("/api/"));
    }

    @Test
    @DisplayName("ninguna referencia a un esquema queda colgada")
    void ningunaReferenciaQuedaColgada() {
        JsonNode contrato = get("/v3/api-docs").json();
        Set<String> declarados = new HashSet<>();
        contrato.get("components").get("schemas").properties()
                .forEach(esquema -> declarados.add(esquema.getKey()));

        List<String> colgadas = referencias(contrato).stream()
                .filter(ref -> ref.startsWith("#/components/schemas/"))
                .map(ref -> ref.substring("#/components/schemas/".length()))
                .filter(nombre -> !declarados.contains(nombre))
                .distinct()
                .toList();

        assertThat(colgadas)
                .withFailMessage("Estos esquemas se referencian y no están declarados: %s", colgadas)
                .isEmpty();
    }

    @Test
    @DisplayName("cada operación documenta los errores que el advice puede devolver")
    void cadaOperacionDocumentaSusErrores() {
        JsonNode rutas = get("/v3/api-docs").json().get("paths");
        List<String> incompletas = new ArrayList<>();

        rutas.properties().forEach(ruta -> ruta.getValue().properties().forEach(metodo -> {
            JsonNode respuestas = metodo.getValue().get("responses");
            for (String codigo : List.of("400", "401", "403", "404", "409", "500")) {
                if (respuestas == null || !respuestas.has(codigo)) {
                    incompletas.add(metodo.getKey().toUpperCase() + " " + ruta.getKey() + " sin " + codigo);
                }
            }
        }));

        // ManejadorErrores aplica a todas las rutas: el contrato no puede prometer menos de lo que
        // pasa.
        assertThat(incompletas).isEmpty();
    }

    private static List<String> referencias(JsonNode nodo) {
        List<String> encontradas = new ArrayList<>();
        if (nodo.isObject()) {
            nodo.properties().forEach(campo -> {
                if (campo.getKey().equals("$ref") && campo.getValue().isTextual()) {
                    encontradas.add(campo.getValue().asText());
                } else {
                    encontradas.addAll(referencias(campo.getValue()));
                }
            });
        } else if (nodo.isArray()) {
            nodo.forEach(hijo -> encontradas.addAll(referencias(hijo)));
        }
        return encontradas;
    }
}
