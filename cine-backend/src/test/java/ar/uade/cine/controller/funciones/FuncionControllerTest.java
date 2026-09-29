package ar.uade.cine.controller.funciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    void borrarUnaFuncionQueNoExisteEs404ConSuMensaje() {
        Respuesta respuesta = pedirComo(HttpMethod.DELETE, "/api/funciones/99", null, EMAIL_ADMIN, CLAVE_ADMIN);

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la función 99", respuesta.error());
    }
}
