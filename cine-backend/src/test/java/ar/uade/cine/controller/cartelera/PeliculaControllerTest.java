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
import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;
import ar.uade.cine.service.usuarios.GestorEmpleados;

class PeliculaControllerTest extends PruebaDeApi {

    @Autowired
    private GestorRevisionCartelera revision;
    @Autowired
    private GestorEmpleados empleados;

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

    // Lo que falta lo corta el pedido, con el mismo texto que daría la película.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            sin título       | {"duracionMinutos":155,"generos":["ACCION"],"clasificacion":"ATP"}             | Falta el título
            título en blanco | {"titulo":" ","duracionMinutos":155,"generos":["ACCION"],"clasificacion":"ATP"} | Falta el título
            sin géneros      | {"titulo":"Dune","duracionMinutos":155,"generos":[],"clasificacion":"ATP"}    | La película tiene que tener al menos un género
            """)
    void unAltaSinTituloOSinGenerosEs400ConElTextoDeLaPelicula(String caso, String cuerpo, String mensaje) {
        Respuesta respuesta = post("/api/peliculas", cuerpo);

        assertEquals(400, respuesta.estado());
        assertEquals(mensaje, respuesta.error());
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

    // Pública, pero lo que el buzón no aprobó no existe sin sesión de administrador: ni para el cliente
    // ni para el acomodador, que no revisa cartelera.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            pendiente sin sesión,             PENDIENTE,  ,                    ,             404
            descartada sin sesión,            DESCARTADA, ,                    ,             404
            confirmada sin sesión,            CONFIRMADA, ,                    ,             200
            pendiente con el acomodador,      PENDIENTE,  puerta@cine.test,    clave-puerta, 404
            pendiente con el administrador,   PENDIENTE,  admin@prueba.test,   clave-de-prueba, 200
            descartada con el administrador,  DESCARTADA, admin@prueba.test,   clave-de-prueba, 200
            """)
    void elDetalleDeLoQueNoPasoElBuzonSoloLoVeElAdministrador(String caso, EstadoRevision estado, String email,
            String clave, int esperado) {
        empleados.registrar("Portero", "puerta@cine.test", "clave-puerta", Rol.ACOMODADOR);
        int id = importadaEn(estado);

        Respuesta respuesta = pedirComo(HttpMethod.GET, "/api/peliculas/" + id, null, email, clave);

        assertEquals(esperado, respuesta.estado());
        if (esperado == 404) {
            assertEquals("No existe la película " + id, respuesta.error());
        } else {
            assertEquals("Dune", respuesta.json().get("titulo").asText());
        }
    }

    private int importadaEn(EstadoRevision estado) {
        int id = revision.importar(DatosPelicula.deAlta("Dune", 155, List.of(Genero.ACCION), Clasificacion.ATP))
                .getId();
        switch (estado) {
            case CONFIRMADA -> revision.confirmar(id);
            case DESCARTADA -> revision.descartar(id);
            case PENDIENTE -> { }
        }
        return id;
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
