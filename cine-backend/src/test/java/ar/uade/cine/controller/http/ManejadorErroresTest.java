package ar.uade.cine.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ar.uade.cine.PruebaDeApi;

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
    void unPedidoIncompletoEs400ConElMensajeDelPrimerCampoQueFalta() {
        Respuesta respuesta = post("/api/clientes", "{}");

        assertEquals(400, respuesta.estado());
        assertEquals("El nombre no puede estar vacío", respuesta.error());
    }

    @Test
    void unEmailSinFormatoEs400() {
        Respuesta respuesta = post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana-sin-arroba\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("El email no es válido", respuesta.error());
    }

    @Test
    void unaFuncionSinPeliculaNiSalaPideLaPeliculaPrimero() {
        Respuesta respuesta = post("/api/funciones", "{\"precio\":5000}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta la película", respuesta.error());
    }

    @Test
    void unClienteConEmailRepetidoEs409() {
        post("/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"ana@mail.com\"}");

        Respuesta respuesta = post("/api/clientes", "{\"nombre\":\"Otra\",\"email\":\"ana@mail.com\"}");

        assertEquals(409, respuesta.estado());
        assertEquals("Ya hay un cliente registrado con ese email", respuesta.error());
    }

    @Test
    void unaFuncionDeUnaPeliculaQueNoExisteEs404() {
        Respuesta respuesta = post("/api/funciones", "{\"peliculaId\":999,\"salaId\":1,"
                + "\"inicio\":\"2026-08-20T20:00:00\",\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000}");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la película 999", respuesta.error());
    }

    @Test
    void unaRutaQueNoExisteEs404() {
        Respuesta respuesta = get("/api/no-existe");

        assertEquals(404, respuesta.estado());
    }
}
