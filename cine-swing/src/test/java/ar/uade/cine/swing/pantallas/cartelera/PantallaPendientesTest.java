package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiImagenes;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// El buzón del importador: decidir de a varias, sin perder las que el backend rechaza.
class PantallaPendientesTest {

    private static String pelicula(int id, String titulo) {
        return "{\"id\":" + id + ",\"titulo\":\"" + titulo + "\",\"duracionMinutos\":100,\"generos\":[\"ACCION\"],"
                + "\"clasificacion\":\"ATP\",\"anio\":2026,\"enCartelera\":false,\"formatos\":[\"DOS_D\"]}";
    }

    private ServidorFalso servidor;
    private Operador encargado;
    private final List<String> preguntas = new CopyOnWriteArrayList<>();
    private final List<String> rechazos = new CopyOnWriteArrayList<>();
    private volatile boolean aceptaDescartar = true;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/peliculas/pendientes", 200,
                "[" + pelicula(7, "Matrix") + "," + pelicula(8, "Coco") + "," + pelicula(9, "Up") + "]");
        servidor.responder("POST /api/peliculas/7/confirmacion", 200, pelicula(7, "Matrix"));
        servidor.responder("POST /api/peliculas/8/confirmacion", 200, pelicula(8, "Coco"));
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new PantallaPendientes(new ApiCartelera(http), new ApiImagenes(http),
                new Navegacion() {
                    @Override public void ir(Destino destino) { }
                    @Override public void volver() { }
                    @Override public void abrirInforme(int funcionId) { }
                    @Override public void abrirCobro(int reservaId) { }
                    @Override public void abrirMapa(int salaId) { }
                }, pregunta -> {
                    preguntas.add(pregunta);
                    return aceptaDescartar;
                }, rechazos::add));
        encargado.esperarTexto("Elegidas: 0 de 3");
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void confirmaSoloLasTildadas() {
        assertFalse(encargado.habilitado("Confirmar elegidas"));

        encargado.tildar("Matrix").tildar("Coco");
        encargado.esperarTexto("Elegidas: 2 de 3");
        encargado.clic("Confirmar elegidas");

        encargado.esperarTexto("Confirmadas: 2");
        encargado.esperarTexto("Elegidas: 0 de 1");
        assertEquals(1, servidor.veces("POST /api/peliculas/7/confirmacion"));
        assertEquals(1, servidor.veces("POST /api/peliculas/8/confirmacion"));
        assertEquals(0, servidor.veces("POST /api/peliculas/9/confirmacion"));
    }

    @Test
    void laQueElBackendRechazaQuedaTildadaYLasDemasSiguen() {
        servidor.responder("POST /api/peliculas/8/confirmacion", 409, "{\"error\":\"Ya se decidió\"}");
        servidor.responder("POST /api/peliculas/9/confirmacion", 200, pelicula(9, "Up"));

        encargado.tildar("Seleccionar todas");
        encargado.esperarTexto("Elegidas: 3 de 3");
        encargado.clic("Confirmar elegidas");

        encargado.esperarTexto("Confirmadas: 2, no se pudieron: 1");
        encargado.esperarTexto("Elegidas: 1 de 1");
        assertTrue(encargado.habilitado("Confirmar elegidas"));
        assertTrue(encargado.hayBoton("Coco"));
        assertFalse(encargado.hayBoton("Matrix"));
        assertEquals(List.of("Coco: Ya se decidió"), rechazos);
    }

    @Test
    void descartarPreguntaConLaCantidadYSinElSiNoManda() {
        servidor.responder("POST /api/peliculas/7/descarte", 200, pelicula(7, "Matrix"));
        encargado.tildar("Matrix");
        encargado.esperarTexto("Elegidas: 1 de 3");

        aceptaDescartar = false;
        encargado.clic("Descartar elegidas");
        assertEquals(List.of("¿Descartar las elegidas (1)? El importador no las vuelve a proponer."), preguntas);
        assertEquals(0, servidor.veces("POST /api/peliculas/7/descarte"));

        aceptaDescartar = true;
        encargado.clic("Descartar elegidas");
        encargado.esperarTexto("Descartadas: 1");
        assertEquals(1, servidor.veces("POST /api/peliculas/7/descarte"));
    }
}
