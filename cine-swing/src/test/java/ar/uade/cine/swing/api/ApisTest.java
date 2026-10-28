package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.candy.PedidoCombo;
import ar.uade.cine.swing.api.dto.candy.PedidoEdicionProducto;
import ar.uade.cine.swing.api.dto.candy.PedidoProducto;
import ar.uade.cine.swing.api.dto.cartelera.PedidoPelicula;
import ar.uade.cine.swing.api.dto.catalogos.TipoProducto;
import ar.uade.cine.swing.api.dto.catalogos.TipoPromocion;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.informes.Bordero;
import ar.uade.cine.swing.api.dto.promociones.PedidoPromocion;
import ar.uade.cine.swing.api.dto.promociones.Promocion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                "{\"id\":1,\"nombre\":\"Encargado\",\"email\":\"encargado@cine.uade.ar\",\"rol\":\"ADMINISTRADOR\",\"empleado\":true}");
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
    void cambiarElEstadoDeUnaButacaDevuelveLaSalaConSusButacas() {
        servidor.responder("PATCH /api/salas/2/asientos/A1", 200, """
                {"id":2,"nombre":"Sala 2","tipo":"DOS_D","butacasPorFila":[1],"filas":1,"capacidadSala":1,
                 "minutosLimpieza":15,"asientos":[{"id":9,"fila":1,"numero":1,"codigo":"A1","tipo":"ESTANDAR",
                 "estado":"FUERA_DE_SERVICIO"}]}
                """);

        Sala sala = new ApiSalas(http).cambiarEstadoAsiento(2, "A1", "FUERA_DE_SERVICIO");

        assertEquals("{\"estado\":\"FUERA_DE_SERVICIO\"}", servidor.ultimo("PATCH /api/salas/2/asientos/A1").cuerpo());
        assertTrue(sala.asientos().get(0).fueraDeServicio());
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
    void elClienteSeBuscaEnUnaListaYSinNadieEsUn404() {
        servidor.responder("GET /api/clientes?email=ana%40mail.com", 200,
                "[{\"id\":3,\"nombre\":\"Ana\",\"email\":\"ana@mail.com\"}]");
        servidor.responder("GET /api/clientes?email=nadie%40mail.com", 200, "[]");
        ApiClientes clientes = new ApiClientes(http);

        assertEquals(3, clientes.buscarClientePorEmail("ana@mail.com").id());
        ErrorApi nadie = assertThrows(ErrorApi.class, () -> clientes.buscarClientePorEmail("nadie@mail.com"));
        assertEquals(404, nadie.estado());
        assertEquals("No hay ningún cliente con el email nadie@mail.com", nadie.getMessage());
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

    @Test
    void subeLaImagenEnMultipartConElCampoArchivo(@TempDir Path carpeta) throws IOException {
        Path archivo = Files.writeString(carpeta.resolve("poster.png"), "PNGDATA");
        servidor.responder("POST /api/imagenes", 201, "{\"url\":\"/api/imagenes/abc.png\"}");

        String url = new ApiImagenes(http).subir(archivo);

        assertEquals("/api/imagenes/abc.png", url);
        ServidorFalso.Recibido pedido = servidor.ultimo();
        assertTrue(pedido.tipoCuerpo().startsWith("multipart/form-data; boundary="));
        String limite = pedido.tipoCuerpo().substring(pedido.tipoCuerpo().indexOf('=') + 1);
        assertEquals("--" + limite + "\r\n"
                + "Content-Disposition: form-data; name=\"archivo\"; filename=\"poster.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n"
                + "PNGDATA\r\n--" + limite + "--\r\n", pedido.cuerpo());
        assertTrue(pedido.autorizacion().startsWith("Basic "));
    }

    @Test
    void laImagenSeDescargaPorLaUrlQueGuardoElBackend() {
        servidor.responder("GET /api/imagenes/abc.png", 200, "PNGDATA");

        byte[] datos = new ApiImagenes(http).descargar("/api/imagenes/abc.png");

        assertEquals("PNGDATA", new String(datos, StandardCharsets.UTF_8));
    }

    @Test
    void editarUnaPromocionMandaElMismoCuerpoQueElAltaPorPut() {
        servidor.responder("PUT /api/promociones/7", 200, "{\"id\":7,\"nombre\":\"Miercoles\",\"activa\":true,"
                + "\"imagenUrl\":\"/api/imagenes/abc.png\"}");
        PedidoPromocion pedido = new PedidoPromocion("Miercoles", "PORCENTAJE", 30.0, null, null, null,
                "2026-10-01", "2026-12-31", List.of("WEDNESDAY"), null, null, List.of(), "/api/imagenes/abc.png");

        Promocion editada = new ApiPromociones(http).editarPromocion(7, pedido);

        assertEquals("/api/imagenes/abc.png", editada.imagenUrl());
        assertEquals("PUT", servidor.ultimo().metodo());
        assertEquals("""
                {"nombre":"Miercoles","tipo":"PORCENTAJE","porcentaje":30.0,"monto":null,"lleva":null,"paga":null,\
                "vigenciaDesde":"2026-10-01","vigenciaHasta":"2026-12-31","diasSemana":["WEDNESDAY"],\
                "horaDesde":null,"horaHasta":null,"mediosPago":[],"imagenUrl":"/api/imagenes/abc.png"}""",
                servidor.ultimo().cuerpo());
    }

    @Test
    void laImagenViajaEnLosPedidosDeProductoYCombo() {
        servidor.responder("POST /api/candy/productos", 201, "{\"id\":1}");
        servidor.responder("POST /api/candy/combos", 201, "{\"id\":2}");
        ApiCandy candy = new ApiCandy(http);

        candy.crearProductoCandy(new PedidoProducto("Pochoclos", "POCHOCLOS", 5000.0, "/api/imagenes/a.png"));
        assertEquals("{\"nombre\":\"Pochoclos\",\"tipo\":\"POCHOCLOS\",\"precio\":5000.0,"
                + "\"imagenUrl\":\"/api/imagenes/a.png\"}", servidor.ultimo().cuerpo());

        candy.armarComboCandy(new PedidoCombo("Combo", 8000.0, Map.of(1, 2), "/api/imagenes/b.png"));
        assertEquals("{\"nombre\":\"Combo\",\"precio\":8000.0,\"componentes\":{\"1\":2},"
                + "\"imagenUrl\":\"/api/imagenes/b.png\"}", servidor.ultimo().cuerpo());
    }

    // El PUT de producto reemplaza todo: un null en imagenUrl es "sin imagen", tiene que viajar.
    @Test
    void editarUnProductoSinImagenMandaImagenUrlEnNull() {
        servidor.responder("PUT /api/candy/productos/3", 200, "{\"id\":3}");

        new ApiCandy(http).editarProductoCandy(3, new PedidoEdicionProducto("Agua", 1500.0, null));

        assertEquals("{\"nombre\":\"Agua\",\"precio\":1500.0,\"imagenUrl\":null}", servidor.ultimo().cuerpo());
    }
}
