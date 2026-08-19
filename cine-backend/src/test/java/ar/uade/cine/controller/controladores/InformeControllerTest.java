package ar.uade.cine.controller.controladores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.usuarios.GestorClientes;
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
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.model.dinero.Dinero;

/**
 * Los dos informes que cortan por función, pedidos por HTTP.
 *
 * <p>Lo que agrega esta prueba sobre {@code GestorInformesTest} es la forma del JSON y el
 * código de respuesta: que el desglose por tarifa viaje con el nombre del enum como clave,
 * que emitir sea 201 y consultar 200, y que una función inexistente sea 404 y no el 400 con
 * el que el gestor rechaza todo. Esa diferencia se decide en {@code api/} y no se ve desde
 * un test de gestor.
 */
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


    private Reserva reserva;

    /** Una función de Matrix con una reserva cobrada: una general y una jubilada. */
    @BeforeEach
    void levantarLaApiConUnaFuncionVendida() {

        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, 1,
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        Cliente cliente = clientes.identificar("Andrei", "andrei@uade.edu.ar");
        reserva = reservas.reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL, "A2", TipoTarifa.JUBILADO));
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
        // La clave es el nombre de la constante, como en todo el contrato.
        assertEquals(1, bordero.get("porTarifa").get("JUBILADO").get("cantidad").asInt());
        assertEquals(2500.0, bordero.get("porTarifa").get("JUBILADO").get("total").asDouble(), 0.001);
    }

    /** Una tarifa sin entradas no viaja en cero: directamente no está, como en el arqueo. */
    @Test
    void elDesgloseSoloTraeLasTarifasConEntradasVendidas() {
        var porTarifa = get("/api/funciones/1/bordero").json().get("porTarifa");

        assertTrue(porTarifa.has("GENERAL"));
        assertFalse(porTarifa.has("ESTUDIANTE"));
    }

    /**
     * Una función que no existe es 404 y no 400. El gestor la rechaza igual, pero con el
     * código de una regla incumplida: distinguirlas es trabajo de la ruta.
     */
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
        assertEquals("El identificador abc no es válido", respuesta.error());
    }

    /** Emitir escribe: por eso es POST y devuelve 201, no 200. */
    @Test
    void emitirElBorderoDevuelve201YDejaElArchivo() {
        Respuesta respuesta = post("/api/funciones/1/bordero", "");

        assertEquals(201, respuesta.estado());
        assertEquals(2, respuesta.json().get("espectadores").asInt());
        assertTrue(Files.exists(Path.of("target/comprobantes/informes").resolve("bordero-funcion-1.txt")));
    }

    /** El informe embebe el borderó completo, no una versión recortada. */
    @Test
    void elInformeSumaLaBoleteriaYElCandyDeLaFuncion() {
        Producto pochoclos = productos
                .agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));
        candy.venderParaReserva(reserva.getId(),
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

    /**
     * La regla que más se va a preguntar cuando alguien mire los números: el mostrador no se
     * atribuye a ninguna función. Se prueba también acá y no solo en el gestor porque es lo
     * que el front va a mostrar como «recaudación de la función».
     */
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
}
