package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.Json;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Borrar siempre pregunta: es destructivo (ver Mensajes). Manda un DELETE a la fila elegida.
class BorrarFuncionTest {

    private ServidorFalso servidor;
    private BorrarFuncion accion;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        accion = new BorrarFuncion(new ApiFunciones(new ClienteHttp(servidor.url())));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    @Test
    void preguntaYBorra() {
        servidor.responder("DELETE /api/funciones/4", 204, "");
        var fila = Json.leer("{\"id\":4,\"inicio\":\"2020-08-13T20:30:00\","
                + "\"pelicula\":{\"id\":1,\"titulo\":\"Matrix\"}}", Funcion.class);

        var pregunta = accion.pregunta(fila).orElseThrow();
        accion.ejecutar(fila);

        assertEquals("¿Borrar la función de Matrix del jueves 13 ago 20:30?", pregunta.texto());
        assertEquals("Sí, borrar", pregunta.si());
        assertEquals("DELETE", servidor.ultimo().metodo());
        assertEquals("/api/funciones/4", servidor.ultimo().ruta());
        assertEquals("Función borrada", accion.hecho(fila));
        assertTrue(accion.habilitada(fila));
    }
}
