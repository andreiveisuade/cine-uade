package ar.uade.cine.controller.comentarios;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;

import ar.uade.cine.PruebaDeApi;

class ComentarioControllerTest extends PruebaDeApi {

    private MockHttpSession ana;

    @BeforeEach
    void anaConLaSesionDeGoogle() {
        ana = entrarConGoogle("ana@mail.com");
    }

    @Test
    @DisplayName("lo que publica el cliente lo ve cualquiera, con su nombre y sin su email")
    void publicaYSeVe() {
        Respuesta alta = publicar("{\"puntaje\":4,\"texto\":\"  Muy buena sala \",\"autor\":\"Otra persona\"}");

        assertThat(alta.estado()).as(alta.cuerpo()).isEqualTo(201);
        JsonNode lista = listar();
        assertThat(lista).hasSize(1);
        JsonNode comentario = lista.get(0);
        assertThat(comentario).isEqualTo(alta.json());
        assertThat(comentario.get("autor").asText()).isEqualTo("Cliente de Google");
        assertThat(comentario.get("puntaje").asInt()).isEqualTo(4);
        assertThat(comentario.get("texto").asText()).isEqualTo("Muy buena sala");
        assertThat(comentario.get("fecha").asText()).startsWith(reloj.hoy().toString());
        assertThat(comentario.toString()).doesNotContain("ana@mail.com");
    }

    @Test
    @DisplayName("el más nuevo va primero")
    void elMasNuevoPrimero() {
        publicar("{\"puntaje\":1,\"texto\":\"Viejo\"}");
        publicar("{\"puntaje\":5,\"texto\":\"Nuevo\"}");

        assertThat(listar().findValuesAsText("texto")).containsExactly("Nuevo", "Viejo");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin puntaje,       '{"texto":"Hola"}',               Falta el puntaje
            sin reseña,        '{"puntaje":3}',                  Falta la reseña
            reseña en blanco,  '{"puntaje":3,"texto":"   "}',    Falta la reseña
            puntaje cero,      '{"puntaje":0,"texto":"Hola"}',   El puntaje tiene que estar entre 1 y 5
            puntaje seis,      '{"puntaje":6,"texto":"Hola"}',   El puntaje tiene que estar entre 1 y 5
            puntaje decimal,   '{"puntaje":4.5,"texto":"Hola"}', El campo puntaje tiene un valor inválido: 4.5
            """)
    void loInvalidoNoSePublica(String caso, String cuerpo, String error) {
        Respuesta respuesta = publicar(cuerpo);

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.error()).isEqualTo(error);
        assertThat(listar()).isEmpty();
    }

    @ParameterizedTest(name = "una reseña de {0} caracteres da {1}")
    @CsvSource({"1000, 201", "1001, 400"})
    void largoMaximo(int largo, int estado) {
        assertThat(publicar("{\"puntaje\":3,\"texto\":\"" + "a".repeat(largo) + "\"}").estado()).isEqualTo(estado);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "'); DROP TABLE comentario; --",
            "' OR '1'='1",
            "x', 5, NOW()), (1, 1, 'colado",
            "<script>alert(1)</script>"})
    @DisplayName("una inyección queda guardada como texto y no toca nada más")
    void laInyeccionEsTexto(String texto) {
        publicar("{\"puntaje\":3,\"texto\":\"Anterior\"}");

        assertThat(publicar("{\"puntaje\":1,\"texto\":\"" + texto + "\"}").estado()).isEqualTo(201);

        assertThat(listar().findValuesAsText("texto")).containsExactly(texto, "Anterior");
    }

    @Test
    @DisplayName("publicar pide la sesión de Google: anónimo 401, empleado 403")
    void publicarPideLaSesionDelCliente() {
        String cuerpo = "{\"puntaje\":5,\"texto\":\"Hola\"}";

        assertThat(pedirComo(HttpMethod.POST, "/api/comentarios", cuerpo, null, null).estado()).isEqualTo(401);
        assertThat(post("/api/comentarios", cuerpo).estado()).isEqualTo(403);
        assertThat(listar()).isEmpty();
    }

    private Respuesta publicar(String cuerpo) {
        return conSesion(HttpMethod.POST, "/api/comentarios", ana, cuerpo);
    }

    private JsonNode listar() {
        return pedirComo(HttpMethod.GET, "/api/comentarios", null, null, null).json();
    }
}
