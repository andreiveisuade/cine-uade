package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.ApiProgramaciones;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.Json;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.api.dto.programaciones.Programacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Lo que alterna pregunta solo al apagar; el botón dice cuál de las dos va a hacer. Manda un PATCH con el contrario.
class CambiarActivacionGrillaTest {

    private ServidorFalso servidor;
    private CambiarActivacionGrilla accion;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        accion = new CambiarActivacionGrilla(new ApiProgramaciones(new ClienteHttp(servidor.url())));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "activa se da de baja preguntando | true  | true  | false | Dar de baja | Grilla dada de baja",
            "dada de baja se reactiva sin preguntar | false | false | true  | Reactivar | Grilla reactivada"})
    void alterna(String caso, boolean prendido, boolean pregunta, boolean pide, String texto, String hecho) {
        servidor.responder("PATCH /api/programaciones/1", 200, "{\"id\":1}");
        var fila = Json.leer("{\"id\":1,\"activa\":" + prendido + "}", Programacion.class);

        assertEquals(texto, accion.texto(fila));
        assertEquals(pregunta, accion.pregunta(fila).isPresent());
        accion.ejecutar(fila);

        assertEquals("PATCH", servidor.ultimo().metodo());
        assertEquals("/api/programaciones/1", servidor.ultimo().ruta());
        assertEquals("{\"activa\":" + pide + "}", servidor.ultimo().cuerpo());
        assertEquals(hecho, accion.hecho(fila));
    }

    @Test
    void laPreguntaDiceQueCambia() {
        var fila = Json.leer("{\"id\":1,\"activa\":true}", Programacion.class);

        var pregunta = accion.pregunta(fila).orElseThrow();

        assertEquals("¿Dar de baja esta grilla? Deja de generar funciones; las que ya generó quedan.",
                pregunta.texto());
        assertEquals("Sí, dar de baja", pregunta.si());
        assertEquals("Dar de baja", accion.texto());
    }
}
