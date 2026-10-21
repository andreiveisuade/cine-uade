package ar.uade.cine.swing.pantallas.salas;

import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.Json;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.api.dto.salas.Sala;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Borrar siempre pregunta: es destructivo (ver Mensajes). Manda un DELETE a la fila elegida.
class BorrarSalaTest {

    private ServidorFalso servidor;
    private BorrarSala accion;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        accion = new BorrarSala(new ApiSalas(new ClienteHttp(servidor.url())));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    @Test
    void preguntaYBorra() {
        servidor.responder("DELETE /api/salas/2", 204, "");
        var fila = Json.leer("{\"id\":2,\"nombre\":\"Sala 2\"}", Sala.class);

        var pregunta = accion.pregunta(fila).orElseThrow();
        accion.ejecutar(fila);

        assertEquals("¿Borrar Sala 2?", pregunta.texto());
        assertEquals("Sí, borrar", pregunta.si());
        assertEquals("DELETE", servidor.ultimo().metodo());
        assertEquals("/api/salas/2", servidor.ultimo().ruta());
        assertEquals("Sala borrada", accion.hecho(fila));
        assertTrue(accion.habilitada(fila));
    }
}
