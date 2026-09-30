package ar.uade.cine.controller.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ar.uade.cine.PruebaDeApi;

import com.fasterxml.jackson.databind.JsonNode;

class ClienteControllerTest extends PruebaDeApi {

    // Daba 500: el alta no veía a los empleados y el INSERT chocaba con el UNIQUE del email.
    @Test
    void registrarConElEmailDeUnEmpleadoEs409() {
        Respuesta respuesta = post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"" + EMAIL_ADMIN + "\"}");

        assertEquals(409, respuesta.estado());
        assertEquals("Ya existe un usuario con ese email", respuesta.error());
    }

    // Un filtro sobre la colección: siempre una lista, con el cliente o vacía; nunca el literal null.
    @Test
    void laBusquedaPorEmailEsUnaListaQueIgnoraLosEspaciosYSinEmailVieneVacia() {
        post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana@mail.com\"}");

        JsonNode encontrados = get("/api/clientes?email=%20ana@mail.com%20").json();
        assertEquals(1, encontrados.size());
        assertEquals("ana@mail.com", encontrados.get(0).get("email").asText());
        assertEquals("[]", get("/api/clientes?email=nadie@mail.com").cuerpo());
        assertEquals("[]", get("/api/clientes").cuerpo());
        assertEquals("[]", get("/api/clientes?email=%20%20").cuerpo());
    }

    // El Location del alta apunta al filtro: seguirlo trae al cliente recién creado.
    @Test
    void elAltaApuntaAlFiltroQueLoEncuentra() {
        Respuesta alta = post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana@mail.com\"}");

        JsonNode encontrados = get(alta.cabeceras().getLocation().toString()).json();
        assertEquals(alta.json().get("id").asInt(), encontrados.get(0).get("id").asInt());
    }
}
