package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.Json;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.api.dto.candy.Producto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Lo que alterna pregunta solo al apagar; el botón dice cuál de las dos va a hacer. Manda un PATCH con el contrario.
class CambiarDisponibilidadProductoTest {

    private ServidorFalso servidor;
    private CambiarDisponibilidadProducto accion;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        accion = new CambiarDisponibilidadProducto(new ApiCandy(new ClienteHttp(servidor.url())));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "a la venta sale preguntando | true  | true  | false | Sacar de la carta | Pochoclo salió de la carta",
            "fuera vuelve sin preguntar | false | false | true  | Reponer | Pochoclo volvió a la carta"})
    void alterna(String caso, boolean prendido, boolean pregunta, boolean pide, String texto, String hecho) {
        servidor.responder("PATCH /api/candy/productos/1", 200, "{\"id\":1}");
        var fila = Json.leer("{\"id\":1,\"nombre\":\"Pochoclo\",\"disponible\":" + prendido + "}", Producto.class);

        assertEquals(texto, accion.texto(fila));
        assertEquals(pregunta, accion.pregunta(fila).isPresent());
        accion.ejecutar(fila);

        assertEquals("PATCH", servidor.ultimo().metodo());
        assertEquals("/api/candy/productos/1", servidor.ultimo().ruta());
        assertEquals("{\"disponible\":" + pide + "}", servidor.ultimo().cuerpo());
        assertEquals(hecho, accion.hecho(fila));
    }

    @Test
    void laPreguntaDiceQueCambia() {
        var fila = Json.leer("{\"id\":1,\"nombre\":\"Pochoclo\",\"disponible\":true}", Producto.class);

        var pregunta = accion.pregunta(fila).orElseThrow();

        assertEquals("¿Sacar Pochoclo de la carta? Deja de venderse en el mostrador.", pregunta.texto());
        assertEquals("Sí, sacar", pregunta.si());
        assertEquals("Sacar de la carta", accion.texto());
    }
}
