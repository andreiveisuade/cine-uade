package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.cartelera.PedidoPelicula;
import ar.uade.cine.swing.api.dto.catalogos.TipoProducto;
import ar.uade.cine.swing.api.dto.catalogos.TipoPromocion;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.informes.Bordero;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lo que manda cada Api de subdominio y cómo lee lo que contesta el backend, contra el mismo servidor falso. */
class ApisTest {

    private ServidorFalso servidor;
    private ClienteHttp http;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        http = new ClienteHttp(servidor.url());
        servidor.responder("POST /api/sesion", 200,
                "{\"id\":1,\"nombre\":\"Encargado\",\"email\":\"encargado@cine.uade.ar\",\"rol\":\"ADMINISTRADOR\"}");
        new ApiSesion(http).login("encargado@cine.uade.ar", "cine2026");
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    @Test
    void parseaLaFuncionConSusEmbebidosEIgnoraCamposNuevos() {
        servidor.responder("GET /api/funciones", 200, """
                [{"id":4,"peliculaId":1,"salaId":2,"inicio":"2026-08-13T20:30:00","idioma":"SUBTITULADA",
                  "proyeccion":"TRES_D","precio":5000,"precioDesde":6500,"campoQueTodaviaNoExiste":true,
                  "sala":{"id":2,"nombre":"Sala 2","tipo":"IMAX","butacasPorFila":[10,10],"filas":2,
                          "capacidadSala":20,"minutosLimpieza":15},
                  "pelicula":{"id":1,"titulo":"Matrix","duracionMinutos":136,"generos":["ACCION"],
                              "clasificacion":"MAS_16","enCartelera":true,"estadoRevision":"CONFIRMADA",
                              "puntaje":8.7,"votos":100}}]
                """);

        List<Funcion> funciones = new ApiFunciones(http).obtenerFunciones(null);

        Funcion funcion = funciones.get(0);
        assertEquals(4, funcion.id());
        assertEquals("Matrix", funcion.pelicula().titulo());
        assertEquals(20, funcion.sala().capacidadSala());
        assertEquals(5000, funcion.precio());
        assertNull(funcion.libres());
    }

    @Test
    void parseaElBorderoConSuMapaPorTarifa() {
        servidor.responder("GET /api/funciones/3/bordero", 200, """
                {"funcionId":3,"pelicula":"Matrix","sala":"Sala 1","funcion":"2026-08-13T20:30:00",
                 "generadoEn":"2026-08-13T19:00:00","espectadores":15,"recaudacionBruta":67500,
                 "descuentos":5000,"recaudacionNeta":62500,
                 "porTarifa":{"GENERAL":{"cantidad":12,"total":60000},"JUBILADO":{"cantidad":3,"total":7500}}}
                """);

        Bordero bordero = new ApiInformes(http).obtenerBordero(3);

        assertEquals(15, bordero.espectadores());
        assertEquals(62500, bordero.recaudacionNeta());
        assertEquals(12, bordero.porTarifa().get("GENERAL").cantidad());
        assertEquals(7500, bordero.porTarifa().get("JUBILADO").total());
    }

    @Test
    void losFiltrosVaciosNoViajanYLosDemasVanCodificados() {
        servidor.responder("GET /api/funciones?salaId=2&desde=2026-08-13", 200, "[]");
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("peliculaId", null);
        filtros.put("salaId", "2");
        filtros.put("desde", " 2026-08-13 ");
        filtros.put("hasta", "");

        new ApiFunciones(http).obtenerFunciones(filtros);

        assertEquals("/api/funciones?salaId=2&desde=2026-08-13", servidor.ultimo().ruta());
        assertEquals("?q=la+casa+%26+el+%C3%B1and%C3%BA", Parametros.consulta(Map.of("q", "la casa & el ñandú")));
    }

    @Test
    void laEdicionParcialNoMandaLosCamposNulos() {
        servidor.responder("PUT /api/peliculas/5", 200, """
                {"id":5,"titulo":"Matrix","duracionMinutos":136,"generos":["ACCION"],"clasificacion":"MAS_16",
                 "enCartelera":false,"estadoRevision":"CONFIRMADA"}
                """);

        new ApiCartelera(http).actualizarPelicula(5, PedidoPelicula.soloPublicacion(false));

        assertEquals("{\"enCartelera\":false}", servidor.ultimo().cuerpo());
    }

    @Test
    void losTiposDeProductoDicenCualEsCombo() {
        servidor.responder("GET /api/tipos-producto", 200, """
                [{"nombre":"POCHOCLOS","esCombo":false},{"nombre":"BEBIDA","esCombo":false},
                 {"nombre":"GOLOSINA","esCombo":false},{"nombre":"COMBO","esCombo":true}]
                """);

        List<TipoProducto> tipos = new ApiCatalogos(http).obtenerTiposProducto();

        assertEquals(List.of("POCHOCLOS", "BEBIDA", "GOLOSINA"),
                tipos.stream().filter(t -> !t.esCombo()).map(TipoProducto::nombre).toList());
    }

    @Test
    void losTiposDePromocionTraenLosCamposQuePide() {
        servidor.responder("GET /api/tipos-promocion", 200, """
                [{"nombre":"PORCENTAJE","campos":["porcentaje"]},{"nombre":"MONTO_FIJO","campos":["monto"]},
                 {"nombre":"NXM","campos":["lleva","paga"]}]
                """);

        List<TipoPromocion> tipos = new ApiCatalogos(http).obtenerTiposPromocion();

        assertEquals(3, tipos.size());
        assertEquals(List.of("lleva", "paga"), tipos.get(2).campos());
    }

    @Test
    void elClienteSeBuscaEnUnaListaQuePuedeVenirVacia() {
        servidor.responder("GET /api/clientes?email=ana%40mail.com", 200,
                "[{\"id\":3,\"nombre\":\"Ana\",\"email\":\"ana@mail.com\"}]");
        servidor.responder("GET /api/clientes?email=nadie%40mail.com", 200, "[]");
        ApiClientes clientes = new ApiClientes(http);

        assertEquals(3, clientes.buscarClientePorEmail("ana@mail.com").orElseThrow().id());
        assertTrue(clientes.buscarClientePorEmail("nadie@mail.com").isEmpty());
    }

    @Test
    void laReservaTraeSiSePuedeCobrarYCancelar() {
        servidor.responder("GET /api/reservas", 200, """
                [{"id":1,"estado":"RESERVADA","codigo":"A","entradas":[],"total":9000,
                  "cobrable":false,"cancelable":true},
                 {"id":2,"estado":"PAGADA","codigo":"B","entradas":[],"total":9000}]
                """);

        List<Reserva> reservas = new ApiVentas(http).obtenerReservas(null);

        assertFalse(reservas.get(0).cobrable());
        assertTrue(reservas.get(0).cancelable());
        // Un backend viejo que no los manda deja los botones apagados, no prendidos.
        assertFalse(reservas.get(1).cobrable());
        assertFalse(reservas.get(1).cancelable());
    }
}
