package ar.uade.cine.controller.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;

class RevisionControllerTest extends PruebaDeApi {

    @Autowired
    private GestorRevisionCartelera revision;

    @Test
    void confirmarLaPublicaYLaSacaDelBuzon() {
        int id = revision.importar(DatosPelicula.deAlta("Dune", 155, List.of(Genero.ACCION), Clasificacion.ATP))
                .getId();
        assertEquals(1, get("/api/peliculas/pendientes").json().size());

        Respuesta respuesta = post("/api/peliculas/" + id + "/confirmacion", "");

        assertEquals(200, respuesta.estado());
        assertEquals("CONFIRMADA", respuesta.json().get("estadoRevision").asText());
        assertEquals(true, respuesta.json().get("enCartelera").asBoolean());
        assertEquals(0, get("/api/peliculas/pendientes").json().size());
    }

    // El controller no busca antes: el 404 lo da el gestor, y esto evita que se vuelva un 400.
    @Test
    void revisarUnaPeliculaQueNoExisteEs404() {
        Respuesta confirmada = post("/api/peliculas/99/confirmacion", "");
        Respuesta descartada = post("/api/peliculas/99/descarte", "");

        assertEquals(404, confirmada.estado());
        assertEquals("No existe la película 99", confirmada.error());
        assertEquals(404, descartada.estado());
        assertEquals("No existe la película 99", descartada.error());
    }
}
