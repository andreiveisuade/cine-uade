package ar.uade.cine.controller.informes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ar.uade.cine.PruebaDeApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.usuarios.GestorEmpleados;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.model.dinero.Dinero;

class InformeControllerTest extends PruebaDeApi {

    @Autowired
    private GestorProductos productos;

    @Autowired
    private GestorCandy candy;

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorClientes clientes;

    @Autowired
    private GestorFunciones funciones;

    @Autowired
    private GestorPagos pagos;

    @Autowired
    private GestorReservas reservas;

    @Autowired
    private GestorSalas salas;

    @Autowired
    private GestorEmpleados empleados;


    private Reserva reserva;

    @BeforeEach
    void levantarLaApiConUnaFuncionVendida() {

        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, 1,
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        Cliente cliente = clientes.identificar("Andrei", "andrei@uade.edu.ar");
        reserva = reservas.reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL, "A2", TipoTarifa.JUBILADO), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
    }


    @Test
    void elBorderoViajaConElDesgloseYLasTresCifras() {
        Respuesta respuesta = get("/api/funciones/1/bordero");

        assertEquals(200, respuesta.estado());
        var bordero = respuesta.json();
        assertEquals("Matrix", bordero.get("pelicula").asText());
        assertEquals("Sala 1", bordero.get("sala").asText());
        assertEquals("2026-08-20T20:00:00", bordero.get("funcion").asText());
        assertEquals(2, bordero.get("espectadores").asInt());
        assertEquals(7500.0, bordero.get("recaudacionBruta").asDouble(), 0.001);
        assertEquals(0.0, bordero.get("descuentos").asDouble(), 0.001);
        assertEquals(7500.0, bordero.get("recaudacionNeta").asDouble(), 0.001);
        assertEquals(1, bordero.get("porTarifa").get("JUBILADO").get("cantidad").asInt());
        assertEquals(2500.0, bordero.get("porTarifa").get("JUBILADO").get("total").asDouble(), 0.001);
    }

    @Test
    void elDesgloseSoloTraeLasTarifasConEntradasVendidas() {
        var porTarifa = get("/api/funciones/1/bordero").json().get("porTarifa");

        assertTrue(porTarifa.has("GENERAL"));
        assertFalse(porTarifa.has("ESTUDIANTE"));
    }

    @Test
    void elBorderoDeUnaFuncionQueNoExisteEs404() {
        Respuesta respuesta = get("/api/funciones/99/bordero");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la función 99", respuesta.error());
    }

    @Test
    void unIdQueNoEsNumeroTampocoRompe() {
        Respuesta respuesta = get("/api/funciones/abc/bordero");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la ruta /api/funciones/abc/bordero", respuesta.error());
    }

    @Test
    void elInformeSumaLaBoleteriaYElCandyDeLaFuncion() {
        Producto pochoclos = productos
                .agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));
        candy.venderParaReserva(reserva.getId(), null,
                Map.of(pochoclos.getId(), 2), MedioPago.EFECTIVO, "");

        Respuesta respuesta = get("/api/funciones/1/informe");

        assertEquals(200, respuesta.estado());
        var informe = respuesta.json();
        assertEquals(7500.0, informe.get("boleteria").get("recaudacionNeta").asDouble(), 0.001);
        assertEquals("Matrix", informe.get("boleteria").get("pelicula").asText());
        assertEquals(1, informe.get("comprasCandy").asInt());
        assertEquals(6000.0, informe.get("candy").asDouble(), 0.001);
        assertEquals(13500.0, informe.get("total").asDouble(), 0.001);
    }

    @Test
    void elCandyDeMostradorNoLlegaAlInformeDeLaFuncion() {
        Producto pochoclos = productos
                .agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));
        candy.vender(null, Map.of(pochoclos.getId(), 1),
                MedioPago.EFECTIVO, "");

        var informe = get("/api/funciones/1/informe").json();

        assertEquals(0, informe.get("comprasCandy").asInt());
        assertEquals(0.0, informe.get("candy").asDouble(), 0.001);
        assertEquals(7500.0, informe.get("total").asDouble(), 0.001);
    }

    @Test
    void laDeclaracionJuradaViajaConElExhibidorLasFuncionesYLosTotales() throws Exception {
        Respuesta respuesta = get("/api/declaracion-jurada?desde=2026-08-20&hasta=2026-08-26");

        assertEquals(200, respuesta.estado());
        assertEquals(new ObjectMapper().readTree("""
                { "exhibidor": {"razonSocial":"Cine UADE S.A.","cuit":"30-71234567-1","numeroExhibidor":"10452"},
                  "desde":"2026-08-20", "hasta":"2026-08-26", "generadaEn":"2026-08-14T10:00:00",
                  "funciones":[{"funcionId":1,"inicio":"2026-08-20T20:00:00","sala":"Sala 1","pelicula":"Matrix",
                                "clasificacion":"MAS_13","idioma":"SUBTITULADA","proyeccion":"DOS_D",
                                "espectadores":2,
                                "porTarifa":{"GENERAL":{"cantidad":1,"total":5000.0},
                                             "JUBILADO":{"cantidad":1,"total":2500.0}},
                                "recaudacionBruta":7500.0,"descuentos":0.0,"recaudacionNeta":7500.0}],
                  "peliculas":[{"titulo":"Matrix","clasificacion":"MAS_13","funciones":1,"espectadores":2,
                                "entradasPorTarifa":{"GENERAL":1,"JUBILADO":1},
                                "recaudacionBruta":7500.0,"descuentos":0.0,"recaudacionNeta":7500.0}],
                  "total":{"funciones":1,"espectadores":2,"entradasPorTarifa":{"GENERAL":1,"JUBILADO":1},
                           "recaudacionBruta":7500.0,"descuentos":0.0,"recaudacionNeta":7500.0} }
                """), respuesta.json());
    }

    @Test
    void laDeclaracionTotalizaCadaPeliculaAparteYSumaTodoEnElTotal() {
        cartelera.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.ATP);
        Funcion funcion = funciones.programar(2, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4000));
        Cliente cliente = clientes.identificar("Andrei", "andrei@uade.edu.ar");
        pagos.cobrar(reservas.reservar(funcion.getId(), cliente.getId(), Map.of("A1", TipoTarifa.MENOR), null).getId(),
                MedioPago.EFECTIVO, "");

        var declaracion = get("/api/declaracion-jurada?desde=2026-08-20&hasta=2026-08-26").json();

        assertEquals(2, declaracion.get("funciones").size());
        assertEquals("DOBLADA", declaracion.get("funciones").get(1).get("idioma").asText());
        var dune = declaracion.get("peliculas").get(0);
        assertEquals("Dune", dune.get("titulo").asText());
        assertEquals("ATP", dune.get("clasificacion").asText());
        assertEquals(1, dune.get("entradasPorTarifa").get("MENOR").asInt());
        assertFalse(dune.get("entradasPorTarifa").has("GENERAL"));
        assertEquals(2400.0, dune.get("recaudacionNeta").asDouble(), 0.001);
        assertEquals("Matrix", declaracion.get("peliculas").get(1).get("titulo").asText());
        var total = declaracion.get("total");
        assertEquals(2, total.get("funciones").asInt());
        assertEquals(3, total.get("espectadores").asInt());
        assertEquals(9900.0, total.get("recaudacionNeta").asDouble(), 0.001);
    }

    @Test
    void sinFechasLaDeclaracionEsLaUltimaSemanaCinematografica() {
        reloj.mover(LocalDateTime.of(2026, 8, 28, 10, 0));

        var declaracion = get("/api/declaracion-jurada").json();

        assertEquals("2026-08-20", declaracion.get("desde").asText());
        assertEquals("2026-08-26", declaracion.get("hasta").asText());
        assertEquals(1, declaracion.get("funciones").get(0).get("funcionId").asInt());
    }

    @Test
    void unPeriodoAlRevesEs400ConElMensajeDelGestor() {
        Respuesta respuesta = get("/api/declaracion-jurada?desde=2026-08-26&hasta=2026-08-20");

        assertEquals(400, respuesta.estado());
        assertEquals("El período tiene que empezar antes de terminar", respuesta.error());
    }

    @Test
    void unaFechaMalEscritaEs400() {
        Respuesta respuesta = get("/api/declaracion-jurada?desde=20-08-2026&hasta=2026-08-26");

        assertEquals(400, respuesta.estado());
        assertEquals("La fecha desde no es válida: usá AAAA-MM-DD", respuesta.error());
    }

    @Test
    void laDeclaracionPideSesionYRolDeAdministrador() {
        empleados.registrar("Portero", "puerta@cine.test", "clave-puerta", Rol.ACOMODADOR);

        assertEquals(401, pedirComo(HttpMethod.GET, "/api/declaracion-jurada", null, null, null).estado());
        assertEquals(403, pedirComo(HttpMethod.GET, "/api/declaracion-jurada", null,
                "puerta@cine.test", "clave-puerta").estado());
    }
}
