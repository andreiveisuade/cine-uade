package ar.uade.cine.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ar.uade.cine.PruebaDeApi;

/**
 * Los errores que no salen de un gestor sino del propio HTTP: que también viajen con
 * código correcto y con la forma {@code {error}} que el front sabe mostrar.
 */
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
