package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.Json;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// Publicar o despublicar: qué pregunta, qué manda (un PUT parcial con solo la publicación) y qué avisa.
class PublicarPeliculaTest {

    private ServidorFalso servidor;
    private PublicarPelicula accion;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        accion = new PublicarPelicula(new ApiCartelera(new ClienteHttp(servidor.url())));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "publicada se despublica preguntando,      true,  true,  false, Despublicada",
            "despublicada se publica sin preguntar,    false, false, true,  Publicada"})
    void alternaLaPublicacion(String caso, boolean enCartelera, boolean pregunta, boolean pide, String hecho) {
        servidor.responder("PUT /api/peliculas/7", 200, "{\"id\":7}");
        Pelicula p = Json.leer("{\"id\":7,\"titulo\":\"Matrix\",\"enCartelera\":" + enCartelera + "}", Pelicula.class);

        assertEquals(pregunta, accion.pregunta(p).isPresent());
        accion.ejecutar(p);

        assertEquals("PUT", servidor.ultimo().metodo());
        assertEquals("/api/peliculas/7", servidor.ultimo().ruta());
        assertEquals("{\"enCartelera\":" + pide + "}", servidor.ultimo().cuerpo());
        assertEquals(hecho, accion.hecho(p));
    }

    @Test
    void laPreguntaDiceQueDejaDeVerse() {
        Pelicula p = Json.leer("{\"id\":7,\"titulo\":\"Matrix\",\"enCartelera\":true}", Pelicula.class);

        var pregunta = accion.pregunta(p).orElseThrow();

        assertEquals("¿Despublicar Matrix? Deja de verse en la cartelera aunque tenga funciones.", pregunta.texto());
        assertEquals("Sí, despublicar", pregunta.si());
        assertFalse(accion.texto().isEmpty());
    }
}
