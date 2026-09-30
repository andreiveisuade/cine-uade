package ar.uade.cine.controller.funciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.salas.GestorSalas;

// R20 por HTTP: el reloj de test está en el 14/08/2026 a las 10:00.
class FuncionControllerTest extends PruebaDeApi {

    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;

    @BeforeEach
    void cargarPeliculaYSala() {
        cartelera.agregar("Interstellar", 120, List.of(Genero.CIENCIA_FICCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10, 10));
    }

    private Respuesta programar(String inicio) {
        return post("/api/funciones", "{\"peliculaId\":1,\"salaId\":1,\"inicio\":\"" + inicio
                + "\",\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\",\"precio\":4500}");
    }

    @Test
    void unaFuncionEnElPasadoEs400ConElMensaje() {
        Respuesta respuesta = programar("2020-01-01T20:00:00");

        assertEquals(400, respuesta.estado());
        assertEquals("La función no puede empezar en el pasado", respuesta.error());
        assertEquals(0, get("/api/funciones").json().size());
    }

    @Test
    void unaFuncionAFuturoEs201() {
        Respuesta respuesta = programar("2026-08-14T20:30:00");

        assertEquals(201, respuesta.estado());
        assertEquals("2026-08-14T20:30:00", respuesta.json().get("inicio").asText());
    }

    // Un mensaje por dato, como en programaciones: "la versión o el formato" no decía cuál faltaba,
    // y la API le dice idioma a lo que el dominio llama versión.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin idioma,     '"proyeccion":"DOS_D"',   Falta el idioma
            sin proyección, '"idioma":"SUBTITULADA"', Falta la proyección
            """)
    void cadaDatoQueFaltaSeNombraSolo(String caso, String formato, String mensaje) {
        Respuesta respuesta = post("/api/funciones", "{\"peliculaId\":1,\"salaId\":1,"
                + "\"inicio\":\"2026-08-14T20:30:00\"," + formato + ",\"precio\":4500}");

        assertEquals(400, respuesta.estado());
        assertEquals(mensaje, respuesta.error());
    }

    // DECIMAL(10,2) no guarda cien millones: sin el tope, MySQL rechazaba el INSERT con un 500.
    @Test
    void unPrecioDeCienMillonesEs400() {
        Respuesta respuesta = post("/api/funciones", "{\"peliculaId\":1,\"salaId\":1,"
                + "\"inicio\":\"2026-08-14T20:30:00\",\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\","
                + "\"precio\":100000000}");

        assertEquals(400, respuesta.estado());
        assertEquals("El precio no puede superar $ 1000000.00", respuesta.error());
        assertEquals(0, get("/api/funciones").json().size());
    }

    // El precio se valida al convertirlo, antes que el resto del pedido y que la búsqueda de la película,
    // como cuando lo rechazaba el DTO.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            precio cero,                        1,  2026-08-14T20:30:00, 0,     El precio tiene que ser mayor a cero
            precio cero y película inexistente, 99, 2026-08-14T20:30:00, 0,     El precio tiene que ser mayor a cero
            precio negativo y fecha inválida,   1,  mañana,              -1,    El precio tiene que ser mayor a cero
            precio con tres decimales,          1,  2026-08-14T20:30:00, 1.005, El precio tiene que tener como máximo 2 decimales
            # Las 20:30:46 no se anuncian en ninguna cartelera.
            inicio con segundos,                1,  2026-08-14T20:30:46, 4500,  La hora de la función tiene que ir sin segundos
            """)
    void unPedidoConUnPrecioOUnInicioQueNoSirvenEs400(String caso, int pelicula, String inicio, String precio,
            String mensaje) {
        Respuesta respuesta = post("/api/funciones", "{\"peliculaId\":" + pelicula + ",\"salaId\":1,\"inicio\":\""
                + inicio + "\",\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\",\"precio\":" + precio + "}");

        assertEquals(400, respuesta.estado());
        assertEquals(mensaje, respuesta.error());
    }

    // Como en la declaración jurada: antes devolvía una lista vacía, que no se distinguía de un período
    // sin funciones.
    @Test
    void buscarConUnDesdePosteriorAlHastaEs400() {
        Respuesta respuesta = get("/api/funciones?desde=2026-08-20&hasta=2026-08-14");

        assertEquals(400, respuesta.estado());
        assertEquals("El período tiene que empezar antes de terminar", respuesta.error());
    }

    @Test
    void borrarUnaFuncionQueNoExisteEs404ConSuMensaje() {
        Respuesta respuesta = pedirComo(HttpMethod.DELETE, "/api/funciones/99", null, EMAIL_ADMIN, CLAVE_ADMIN);

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la función 99", respuesta.error());
    }
}
