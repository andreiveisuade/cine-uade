package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Programar una función suelta y filtrar el listado: qué manda, qué muestra y qué rechaza antes de pedir.
class PantallaFuncionesTest {

    static final String SALA = "{\"id\":1,\"nombre\":\"Sala 1\",\"tipo\":\"DOS_D\",\"butacasPorFila\":[10],"
            + "\"filas\":1,\"capacidadSala\":10,\"minutosLimpieza\":15}";
    static final String PELICULA = "{\"id\":7,\"titulo\":\"Matrix\",\"duracionMinutos\":136,\"generos\":[],"
            + "\"clasificacion\":\"MAS_13\",\"enCartelera\":true}";

    static String funcion(int id, String inicio) {
        return "{\"id\":" + id + ",\"peliculaId\":7,\"salaId\":1,\"inicio\":\"" + inicio + "\","
                + "\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000.0,"
                + "\"sala\":" + SALA + ",\"pelicula\":" + PELICULA + "}";
    }

    private ServidorFalso servidor;
    private Operador encargado;
    private final List<Integer> informesAbiertos = new ArrayList<>();

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/peliculas", 200, "[" + PELICULA + "]");
        servidor.responder("GET /api/salas", 200, "[" + SALA + "]");
        servidor.responder("GET /api/idiomas", 200, "[\"DOBLADA\",\"SUBTITULADA\"]");
        servidor.responder("GET /api/proyecciones", 200, "[\"DOS_D\",\"TRES_D\"]");
        servidor.responder("GET /api/funciones", 200,
                "[" + funcion(1, "2026-11-04T20:30:00") + "," + funcion(2, "2026-11-05T18:00:00") + "]");
        ClienteHttp http = new ClienteHttp(servidor.url());
        Navegacion navegacion = new Navegacion() {
            @Override
            public void ir(Destino destino) {
            }

            @Override
            public void abrirInforme(int funcionId) {
                informesAbiertos.add(funcionId);
            }

            @Override
            public void abrirCobro(int reservaId) {
            }

            @Override
            public void volver() {
            }

            @Override
            public void abrirMapa(int salaId) {
            }
        };
        encargado = Operador.en(() -> new PantallaFunciones(new ApiCartelera(http), new ApiCatalogos(http),
                new ApiFunciones(http), new ApiSalas(http), navegacion));
        encargado.esperarFilas(2);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void listaLasFuncionesConSuFormato() {
        encargado.esperarTexto("2 programadas");

        assertEquals("Matrix", encargado.celda(0, "Película"));
        assertEquals("2D · Subtitulada", encargado.celda(0, "Formato"));
        assertEquals(List.of("Todas", "Matrix"), encargado.opciones("Película"));
    }

    @Test
    void filtrarPorSalaPideSoloEsaSala() {
        servidor.responder("GET /api/funciones?salaId=1", 200, "[" + funcion(1, "2026-11-04T20:30:00") + "]");

        encargado.elegir("Sala", "Sala 1");

        encargado.esperarTexto("mostrando 1 de 2");
    }

    @Test
    void programaConElInicioArmadoDelDiaYLaHora() {
        servidor.responder("POST /api/funciones", 201, funcion(3, "2026-11-10T21:15:00"));
        encargado.elegir("Película *", "Matrix (136′)")
                .elegir("Sala *", "Sala 1 — 2D")
                .fecha("Día", LocalDate.of(2026, 11, 10))
                .hora("Hora", LocalTime.of(21, 15))
                .elegir("Idioma", "Doblada")
                .elegir("Proyección", "3D")
                .tipear("Precio base", "5500,50")
                .clic("Programar");

        encargado.esperarTexto("Función programada");
        assertEquals("{\"peliculaId\":7,\"salaId\":1,\"inicio\":\"2026-11-10T21:15:00\",\"idioma\":\"DOBLADA\","
                + "\"proyeccion\":\"TRES_D\",\"precio\":5500.5}", servidor.ultimo("POST /api/funciones").cuerpo());
    }

    @Test
    void sinPrecioNiDiaNoSeProgramaNada() {
        encargado.fecha("Día", null).clic("Programar");

        encargado.esperarTexto("Falta completar «Día»");
        encargado.esperarTexto("Falta completar «Precio base»");
        assertEquals(0, servidor.veces("POST /api/funciones"));
    }

    @Test
    void elSolapamientoQueRechazaElBackendSeMuestraTalCual() {
        servidor.responder("POST /api/funciones", 400,
                "{\"error\":\"La sala está ocupada de 20:30 a 23:01 por Matrix\"}");
        encargado.tipear("Precio base", "5000").clic("Programar");

        encargado.esperarTexto("La sala está ocupada de 20:30 a 23:01 por Matrix");
    }

    @Test
    void elBotonDeInformesAbreElDeLaFuncionElegida() {
        encargado.elegirFila(1).clic("Borderó e informe");

        assertEquals(List.of(2), informesAbiertos);
    }
}
