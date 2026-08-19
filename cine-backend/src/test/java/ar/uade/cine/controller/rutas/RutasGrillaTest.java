package ar.uade.cine.controller.rutas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.Aplicacion;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.controller.ApiEnMemoria;

/**
 * El armado de la grilla, pedido por HTTP.
 *
 * <p>Lo que agrega sobre {@code PlanificadorGrillaTest} es qué pasa con el cuerpo del
 * pedido: qué campos tienen default, cuál es obligatorio y con qué mensaje se rechaza. Eso
 * se decide en {@code api/} y no se ve desde un test del planificador, que recibe los
 * criterios ya armados.
 */
class RutasGrillaTest {

    @TempDir
    Path tempDir;

    private ApiEnMemoria api;

    @BeforeEach
    void levantarLaApiConUnCine() {
        api = new ApiEnMemoria(tempDir);
        Aplicacion app = api.aplicacion();

        Pelicula matrix = app.getCartelera()
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        matrix.setPuntaje(8.7);
        app.getCartelera().actualizar(matrix);
        app.getSalas().agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
    }

    @AfterEach
    void bajarLaApi() {
        api.close();
    }

    /**
     * El resto de los campos sí tiene default: con solo el precio alcanza para una semana
     * de 14 a 24 con ocho títulos, que es lo que promete el contrato.
     */
    @Test
    void conSoloElPrecioArmaLaGrillaConLosDefaults() {
        ApiEnMemoria.Respuesta respuesta = api.post("/api/grilla/propuesta", "{\"precio\":5000}");

        assertEquals(200, respuesta.estado());
        assertTrue(respuesta.json().get("pases").size() > 0);
        assertEquals(0, respuesta.json().get("funcionesCreadas").asInt(),
                "previsualizar no escribe: el contador queda en cero");
    }

    /**
     * El precio no tiene default a propósito, y el mensaje tiene que decir que falta y no
     * que es inválido: sin esto, mandar el cuerpo vacío respondía "el precio debe ser mayor
     * a cero" sobre un precio que nadie mandó.
     */
    @Test
    void sinPrecioAvisaQueFaltaYNoQueEsInvalido() {
        ApiEnMemoria.Respuesta respuesta = api.post("/api/grilla/propuesta", "{}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta el precio de las funciones", respuesta.json().get("error").asText());
    }

    /** Un precio mandado pero imposible sigue siendo el otro error, el del planificador. */
    @Test
    void conPrecioEnCeroElMensajeEsElDelPlanificador() {
        ApiEnMemoria.Respuesta respuesta = api.post("/api/grilla/propuesta", "{\"precio\":0}");

        assertEquals(400, respuesta.estado());
        assertEquals("El precio debe ser mayor a cero", respuesta.json().get("error").asText());
    }

    /** El alta sí escribe, y lo dice en el mismo campo que la previsualización deja en cero. */
    @Test
    void elAltaDevuelve201YCuentaLasFuncionesCreadas() {
        ApiEnMemoria.Respuesta respuesta = api.post("/api/grilla",
                "{\"precio\":5000,\"dias\":1,\"cuantasPeliculas\":1}");

        assertEquals(201, respuesta.estado());
        int creadas = respuesta.json().get("funcionesCreadas").asInt();
        assertTrue(creadas > 0, "el alta tiene que crear funciones");
        assertEquals(creadas, api.aplicacion().getFunciones().listar().size());
    }
}
