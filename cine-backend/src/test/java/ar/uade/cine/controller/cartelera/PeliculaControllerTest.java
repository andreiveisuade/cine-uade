package ar.uade.cine.controller.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;

class PeliculaControllerTest extends PruebaDeApi {

    @Autowired
    private GestorRevisionCartelera revision;

    // El controller no busca antes: el 404 lo da el gestor, y esto evita que se vuelva un 400.
    @Test
    void editarOBorrarUnaPeliculaQueNoExisteEs404() {
        Respuesta editada = put("/api/peliculas/99", "{\"titulo\":\"Dune\"}");
        Respuesta borrada = pedirComo(HttpMethod.DELETE, "/api/peliculas/99", null, EMAIL_ADMIN, CLAVE_ADMIN);

        assertEquals(404, editada.estado());
        assertEquals("No existe la película 99", editada.error());
        assertEquals(404, borrada.estado());
        assertEquals("No existe la película 99", borrada.error());
    }

    // El pedido solo mira que venga: el rango lo pone la película, así el alta y la edición dicen lo mismo.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            cero,                 0
            el entero más grande, 2147483647
            """)
    void unaDuracionFueraDeRangoEs400ConElTextoDeLaPelicula(String caso, int minutos) {
        Respuesta respuesta = post("/api/peliculas", "{\"titulo\":\"Dune\",\"duracionMinutos\":" + minutos
                + ",\"generos\":[\"ACCION\"],\"clasificacion\":\"ATP\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("La duración tiene que estar entre 1 y 600 minutos", respuesta.error());
    }

    @Test
    void publicarUnaPendienteEs400YSigueSinPublicar() {
        int id = revision.importar(DatosPelicula.deAlta("Dune", 155, List.of(Genero.ACCION), Clasificacion.ATP))
                .getId();

        Respuesta respuesta = put("/api/peliculas/" + id, "{\"enCartelera\":true}");

        assertEquals(400, respuesta.estado());
        assertEquals("La película Dune no está confirmada: revisala antes de publicarla", respuesta.error());
        assertFalse(get("/api/peliculas/" + id).json().get("enCartelera").asBoolean());
    }
}
