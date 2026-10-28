package ar.uade.cine.swing.pantallas.cartelera;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// El catálogo de películas: el listado con sus filtros y el alta o la edición al costado.
class PantallaPeliculasTest {

    private static final String MATRIX = "{\"id\":7,\"titulo\":\"Matrix\",\"duracionMinutos\":136,"
            + "\"generos\":[\"ACCION\",\"CIENCIA_FICCION\"],\"clasificacion\":\"MAS_13\",\"director\":\"Wachowski\","
            + "\"anio\":1999,\"idiomaOriginal\":\"Inglés\",\"sinopsis\":\"Neo\",\"enCartelera\":true,"
            + "\"trailerYoutube\":\"abc\"}";
    private static final String COCO = "{\"id\":8,\"titulo\":\"Coco\",\"duracionMinutos\":105,"
            + "\"generos\":[\"ANIMACION\"],\"clasificacion\":\"ATP\",\"anio\":0,\"enCartelera\":false}";

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/generos", 200, "[\"ACCION\",\"ANIMACION\",\"CIENCIA_FICCION\"]");
        servidor.responder("GET /api/clasificaciones", 200,
                "[{\"nombre\":\"ATP\",\"edadMinima\":0},{\"nombre\":\"MAS_13\",\"edadMinima\":13}]");
        servidor.responder("GET /api/peliculas", 200, "[" + MATRIX + "," + COCO + "]");
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new PantallaPeliculas(new ApiCartelera(http), new ApiCatalogos(http)));
        encargado.esperarFilas(2);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void listaElCatalogoConSuConteo() {
        encargado.esperarTexto("2 cargadas · 1 publicadas");

        assertEquals("Matrix (1999)", encargado.celda(0, "Título"));
        assertEquals("Acción, Ciencia ficción", encargado.celda(0, "Géneros"));
        assertEquals("Coco", encargado.celda(1, "Título"));
        assertEquals("—", encargado.celda(1, "Dirección"));
        assertEquals("Despublicada", encargado.celda(1, "Estado"));
    }

    @Test
    void filtrarPorGeneroPideSoloEseYCuentaLoQueQueda() {
        servidor.responder("GET /api/peliculas?genero=ANIMACION", 200, "[" + COCO + "]");

        encargado.elegir("Género", "Animación");

        encargado.esperarFilas(1);
        encargado.esperarTexto("2 cargadas · 1 publicadas · mostrando 1");
        assertEquals("Coco", encargado.celda(0, "Título"));
    }

    @Test
    void agregaUnaPeliculaConLoTipeado() {
        servidor.responder("POST /api/peliculas", 201, COCO);
        encargado.tipear("Título", "Coco")
                .tipear("Duración (min)", "105")
                .elegir("Clasificación", "ATP — todo público")
                .tipear("Trailer (YouTube)", " xyz ")
                .tildar("Animación")
                .tildar("Publicada")
                .clic("Agregar");

        encargado.esperarTexto("Película agregada");
        assertEquals("{\"titulo\":\"Coco\",\"duracionMinutos\":105,\"generos\":[\"ANIMACION\"],"
                        + "\"clasificacion\":\"ATP\",\"director\":\"\",\"sinopsis\":\"\",\"idiomaOriginal\":\"\","
                        + "\"posterUrl\":\"\",\"enCartelera\":false,\"trailerYoutube\":\"xyz\"}",
                servidor.ultimo("POST /api/peliculas").cuerpo());
    }

    @Test
    void sinTituloDuracionNiGeneroNoSeManda() {
        encargado.clic("Agregar");

        encargado.esperarTexto("Falta completar «Título»");
        encargado.esperarTexto("Falta completar «Duración»");
        encargado.esperarTexto("Falta completar «Géneros»");
        assertEquals(0, servidor.veces("POST /api/peliculas"));
    }

    @Test
    void editarPrecargaYGuardaConPut() {
        servidor.responder("PUT /api/peliculas/7", 200, MATRIX);
        encargado.elegirFila(0).clic("Editar");

        assertTrue(encargado.textos().contains("Editar Matrix"));
        assertEquals("136", encargado.valor("Duración (min)"));
        assertEquals("1999", encargado.valor("Año"));
        assertEquals("Wachowski", encargado.valor("Dirección"));
        assertEquals("abc", encargado.valor("Trailer (YouTube)"));

        encargado.tipear("Dirección", "").clic("Guardar cambios");

        encargado.esperarTexto("Cambios guardados");
        String cuerpo = servidor.ultimo("PUT /api/peliculas/7").cuerpo();
        assertTrue(cuerpo.contains("\"generos\":[\"ACCION\",\"CIENCIA_FICCION\"],\"clasificacion\":\"MAS_13\","
                + "\"director\":\"\""), "un director borrado viaja vacío para borrarlo: " + cuerpo);
        assertTrue(cuerpo.contains("\"anio\":1999"), cuerpo);
    }

    @Test
    void elRechazoDelBackendSeMuestraTalCualYNoPierdeLoTipeado() {
        servidor.responder("POST /api/peliculas", 409, "{\"error\":\"Ya hay una película con el título Coco\"}");
        encargado.tipear("Título", "Coco").tipear("Duración (min)", "105").tildar("Animación").clic("Agregar");

        encargado.esperarTexto("Ya hay una película con el título Coco");
        assertEquals("Coco", encargado.valor("Título"));
    }
}
