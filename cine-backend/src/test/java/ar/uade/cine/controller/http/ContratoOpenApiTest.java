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

        // Sin número fijo: un contrato vacío también devolvería 200.
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
    @DisplayName("una ruta pública no pide credencial ni promete 401")
    void rutaPublicaSinCandado() {
        JsonNode operacion = operacion("/api/cartelera", "get");

        assertThat(operacion.get("security")).isNotNull().isEmpty();
        assertThat(operacion.get("responses").has("401")).isFalse();
    }

    @Test
    @DisplayName("una ruta protegida documenta 401 y 403")
    void rutaProtegidaConCandado() {
        JsonNode respuestas = operacion("/api/salas", "post").get("responses");

        assertThat(respuestas.has("401")).isTrue();
        assertThat(respuestas.has("403")).isTrue();
    }

    @Test
    @DisplayName("el 409 queda donde se compite por una butaca o hay un nombre único")
    void conflictoSoloAlReservarOConNombreUnico() {
        assertThat(operacion("/api/generos", "get").get("responses").has("409")).isFalse();
        assertThat(operacion("/api/funciones", "post").get("responses").has("409")).isFalse();
        assertThat(operacion("/api/reservas", "post").get("responses").has("409")).isTrue();
        assertThat(operacion("/api/salas", "post").get("responses").has("409")).isTrue();
    }

    @Test
    @DisplayName("sin variable de ruta no hay 404, y sin filtros una lectura no tiene 400")
    void erroresSegunLaForma() {
        JsonNode generos = operacion("/api/generos", "get").get("responses");
        assertThat(generos.has("404")).isFalse();
        assertThat(generos.has("400")).isFalse();
        assertThat(operacion("/api/peliculas/{id}", "get").get("responses").has("404")).isTrue();
    }

    private JsonNode operacion(String ruta, String metodo) {
        return get("/v3/api-docs").json().get("paths").get(ruta).get(metodo);
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
