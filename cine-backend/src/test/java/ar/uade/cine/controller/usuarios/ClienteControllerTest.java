package ar.uade.cine.controller.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ar.uade.cine.PruebaDeApi;

class ClienteControllerTest extends PruebaDeApi {

    // Daba 500: el alta no veía a los empleados y el INSERT chocaba con el UNIQUE del email.
    @Test
    void registrarConElEmailDeUnEmpleadoEs409() {
        Respuesta respuesta = post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"" + EMAIL_ADMIN + "\"}");

        assertEquals(409, respuesta.estado());
        assertEquals("Ese email es de un empleado del cine", respuesta.error());
    }

    @Test
    void laBusquedaPorEmailIgnoraLosEspaciosYSinEmailDevuelveNull() {
        post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana@mail.com\"}");

        assertEquals("ana@mail.com", get("/api/clientes?email=%20ana@mail.com%20").json().get("email").asText());
        assertEquals("null", get("/api/clientes").cuerpo());
        assertEquals("null", get("/api/clientes?email=%20%20").cuerpo());
    }
}
