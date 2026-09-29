package ar.uade.cine.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.RequestDispatcher;

// Lo que llega a /error sin pasar por ManejadorErrores; el firewall, de punta a punta, está en SeguridadTest.
class ErroresControllerTest {

    // Una excepción que escapa de un filtro llega con 500: el detalle lo registra el servidor, no el usuario.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            excepción que escapa de un filtro, 500, Ocurrió un error inesperado en el servidor
            rechazo del firewall,              400, El pedido no es válido
            ruta que el servidor no encontró,  404, No existe lo que se pidió
            """)
    void contestaEnCastellanoSegunElStatus(String caso, int estado, String error) {
        MockHttpServletRequest pedido = new MockHttpServletRequest("GET", "/error");
        pedido.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, estado);

        var respuesta = new ErroresController().error(pedido);

        assertEquals(estado, respuesta.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_JSON, respuesta.getHeaders().getContentType());
        assertEquals(error, respuesta.getBody().error());
    }

    @Test
    void pedirErrorDirectoEs500ComoEnBoot() {
        var respuesta = new ErroresController().error(new MockHttpServletRequest("GET", "/error"));

        assertEquals(500, respuesta.getStatusCode().value());
        assertEquals("Ocurrió un error inesperado en el servidor", respuesta.getBody().error());
    }
}
