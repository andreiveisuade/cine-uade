package ar.uade.cine.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ar.uade.cine.PruebaDeApi;

/** Los errores del propio HTTP también viajan con su código y la forma {@code {error}}. */
class ManejadorErroresTest extends PruebaDeApi {

    @Test
    void credencialesEquivocadasSon401() {
        Respuesta respuesta = post("/api/sesion", "{\"email\":\"nadie@cine.com\",\"password\":\"otracosa\"}");

        assertEquals(401, respuesta.estado());
        assertEquals("Email o contraseña incorrectos", respuesta.error());
    }

    @Test
    void unMetodoQueLaRutaNoAceptaEs405() {
        Respuesta respuesta = put("/api/cartelera", "{}");

        assertEquals(405, respuesta.estado());
        assertEquals("La ruta no acepta PUT", respuesta.error());
    }

    @Test
    void unCuerpoQueNoEsJsonEs400() {
        Respuesta respuesta = post("/api/salas", "{nombre:");

        assertEquals(400, respuesta.estado());
        assertEquals("El cuerpo del pedido no es un JSON válido", respuesta.error());
    }

    @Test
    void unaRutaQueNoExisteEs404() {
        Respuesta respuesta = get("/api/no-existe");

        assertEquals(404, respuesta.estado());
    }
}
