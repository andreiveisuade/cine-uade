package ar.uade.cine.controller.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.service.usuarios.GestorClientes;

import com.fasterxml.jackson.databind.JsonNode;

class ClienteControllerTest extends PruebaDeApi {

    @Autowired
    private GestorClientes clientes;

    // Un filtro sobre la colección: siempre una lista, con el cliente o vacía; nunca el literal null.
    @Test
    void laBusquedaPorEmailEsUnaListaQueIgnoraLosEspaciosYSinEmailVieneVacia() {
        clientes.identificar("Ana", "ana@mail.com");

        JsonNode encontrados = get("/api/clientes?email=%20ana@mail.com%20").json();
        assertEquals(1, encontrados.size());
        assertEquals("ana@mail.com", encontrados.get(0).get("email").asText());
        assertEquals("[]", get("/api/clientes?email=nadie@mail.com").cuerpo());
        assertEquals("[]", get("/api/clientes").cuerpo());
        assertEquals("[]", get("/api/clientes?email=%20%20").cuerpo());
    }
}
