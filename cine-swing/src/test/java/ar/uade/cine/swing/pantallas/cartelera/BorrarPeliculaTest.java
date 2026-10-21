package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.Json;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Borrar siempre pregunta: es destructivo (ver Mensajes). Manda un DELETE a la fila elegida.
class BorrarPeliculaTest {

    private ServidorFalso servidor;
    private BorrarPelicula accion;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        accion = new BorrarPelicula(new ApiCartelera(new ClienteHttp(servidor.url())));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    @Test
    void preguntaYBorra() {
        servidor.responder("DELETE /api/peliculas/7", 204, "");
        var fila = Json.leer("{\"id\":7,\"titulo\":\"Matrix\"}", Pelicula.class);

        var pregunta = accion.pregunta(fila).orElseThrow();
        accion.ejecutar(fila);

        assertEquals("¿Borrar Matrix?", pregunta.texto());
        assertEquals("Sí, borrar", pregunta.si());
        assertEquals("DELETE", servidor.ultimo().metodo());
        assertEquals("/api/peliculas/7", servidor.ultimo().ruta());
        assertEquals("Película borrada", accion.hecho(fila));
        assertTrue(accion.habilitada(fila));
    }
}
