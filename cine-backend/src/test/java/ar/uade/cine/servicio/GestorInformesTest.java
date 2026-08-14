package ar.uade.cine.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PagoDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProductoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PromocionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;

/**
 * El borderó del INCAA y el informe financiero de una función. Las dos preguntas se
 * responden con lo <strong>cobrado</strong>, y casi todos los casos de borde de acá son
 * plata que parece de la función y no lo es.
 */
class GestorInformesTest {

    @TempDir
    Path tempDir;

    private Path directorioInformes;
    private GestorReservas reservas;
    private GestorPagos pagos;
    private GestorPromociones promociones;
    private GestorProductos productos;
    private GestorCandy candy;
    private GestorInformes informes;

    /** Dos funciones de Matrix en la misma sala 2D de 10 butacas, a $5000, y un cliente. */
    @BeforeEach
    void prepararEscenario() {
        PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
        SalaDAO salaDAO = new SalaDAOMemoria();
        AsientoDAO asientoDAO = new AsientoDAOMemoria();
        FuncionDAO funcionDAO = new FuncionDAOMemoria();
        ClienteDAO clienteDAO = new ClienteDAOMemoria();
        PagoDAO pagoDAO = new PagoDAOMemoria();
        ReservaDAO reservaDAO = new ReservaDAOMemoria();
        CompraCandyDAO compraCandyDAO = new CompraCandyDAOMemoria();
        directorioInformes = tempDir.resolve("informes");

        GestorFunciones gestorFunciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        new GestorCartelera(peliculaDAO, funcionDAO, new GestorProgramaciones(
                new ProgramacionDAOMemoria(), funcionDAO, gestorFunciones))
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        new GestorSalas(salaDAO, asientoDAO, funcionDAO).agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        gestorFunciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, 5000);
        gestorFunciones.programar(1, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, 5000);
        new GestorClientes(clienteDAO, reservaDAO, compraCandyDAO)
                .registrar("Andrei", "andrei@uade.edu.ar");

        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO, peliculaDAO,
                new GeneradorTicketTxt(tempDir.resolve("tickets")), new CalculadoraPrecio(),
                new Ocupacion(reservaDAO, funcionDAO, asientoDAO));
        promociones = new GestorPromociones(new PromocionDAOMemoria());
        pagos = new GestorPagos(pagoDAO, reservaDAO, funcionDAO, promociones, new MercadoPagoEmulado(),
                new GeneradorReciboTxt(tempDir.resolve("tickets")));
        productos = new GestorProductos(new ProductoDAOMemoria());
        candy = new GestorCandy(compraCandyDAO, clienteDAO, reservaDAO,
                new GeneradorTicketCandyTxt(tempDir.resolve("tickets")), productos);
        informes = new GestorInformes(funcionDAO, peliculaDAO, salaDAO, reservaDAO, pagoDAO,
                compraCandyDAO, new GeneradorBorderoTxt(directorioInformes));
    }

    // ---------- el borderó ----------

    @Test
    void elBorderoTitulaConLaPeliculaLaSalaYElHorarioDeLaFuncion() {
        Bordero bordero = informes.borderoDe(1);

        assertEquals("Matrix", bordero.pelicula());
        assertEquals("Sala 1", bordero.sala());
        assertEquals(LocalDateTime.of(2026, 8, 20, 20, 0), bordero.funcion());
    }

    /** Lo que se declara es lo que se cobró: una reserva sin pagar no vendió ninguna entrada. */
    @Test
    void elBorderoNoCuentaLasReservasSinPagar() {
        Reserva cobrada = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        reservas.reservar(1, 1, butacas("A2", TipoTarifa.GENERAL));
        pagos.cobrar(cobrada.getId(), MedioPago.EFECTIVO, "");

        Bordero bordero = informes.borderoDe(1);

        assertEquals(1, bordero.espectadores());
        assertEquals(5000.0, bordero.recaudacionNeta(), 0.001);
    }

    @Test
    void elBorderoNoCuentaLasEntradasDeOtraFuncion() {
        Reserva primera = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        Reserva otraFuncion = reservas.reservar(2, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(primera.getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(otraFuncion.getId(), MedioPago.EFECTIVO, "");

        assertEquals(1, informes.borderoDe(1).espectadores());
        assertEquals(1, informes.borderoDe(2).espectadores());
    }

    /** El desglose por tarifa es lo que el organismo mira: qué se vendió a precio reducido. */
    @Test
    void elBorderoDesglosaCuantasEntradasSalieronACadaTarifa() {
        Map<String, TipoTarifa> pedido = new LinkedHashMap<>();
        pedido.put("A1", TipoTarifa.GENERAL);
        pedido.put("A2", TipoTarifa.GENERAL);
        pedido.put("A3", TipoTarifa.JUBILADO);
        Reserva reserva = reservas.reservar(1, 1, pedido);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Bordero bordero = informes.borderoDe(1);

        assertEquals(3, bordero.espectadores());
        assertEquals(2, bordero.porTarifa().get(TipoTarifa.GENERAL).cantidad());
        assertEquals(10000.0, bordero.porTarifa().get(TipoTarifa.GENERAL).total(), 0.001);
        assertEquals(1, bordero.porTarifa().get(TipoTarifa.JUBILADO).cantidad());
        // La jubilada sale la mitad, y por eso el promedio por espectador no alcanza para
        // reconstruir este desglose.
        assertEquals(2500.0, bordero.porTarifa().get(TipoTarifa.JUBILADO).total(), 0.001);
        assertEquals(12500.0, bordero.recaudacionBruta(), 0.001);
    }

    /**
     * El bruto es a precio de lista y el neto lo que entró: la promoción es un descuento
     * comercial del cine y no cambia el valor declarado de la localidad.
     */
    @Test
    void elBorderoSeparaElBrutoDelDescuentoYDelNeto() {
        promociones.crearPorcentaje("50 off", 50,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Bordero bordero = informes.borderoDe(1);

        assertEquals(5000.0, bordero.recaudacionBruta(), 0.001);
        assertEquals(2500.0, bordero.descuentos(), 0.001);
        assertEquals(2500.0, bordero.recaudacionNeta(), 0.001);
    }

    /** Una función que no vendió nada se declara igual: cero también es una declaración. */
    @Test
    void elBorderoDeUnaFuncionSinVentasDaEnCero() {
        Bordero bordero = informes.borderoDe(1);

        assertEquals(0, bordero.espectadores());
        assertEquals(0, bordero.recaudacionBruta(), 0.001);
        assertEquals(0, bordero.recaudacionNeta(), 0.001);
        assertTrue(bordero.porTarifa().isEmpty());
    }

    @Test
    void noHayBorderoDeUnaFuncionQueNoExiste() {
        assertThrows(IllegalArgumentException.class, () -> informes.borderoDe(99));
    }

    @Test
    void exportarEscribeElArchivoQueSeSubeAlIncaa() {
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        informes.exportarBordero(1);

        Path archivo = directorioInformes.resolve("bordero-funcion-1.txt");
        assertTrue(Files.exists(archivo));
        String texto = leer(archivo);
        assertTrue(texto.contains("Matrix"));
        assertTrue(texto.contains("Sala 1"));
        assertTrue(texto.contains("GENERAL"));
        assertTrue(texto.contains("5000.00"));
    }

    /** El borderó de una función es uno solo: el último emitido pisa al anterior. */
    @Test
    void volverAExportarActualizaElMismoArchivo() {
        Reserva primera = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(primera.getId(), MedioPago.EFECTIVO, "");
        informes.exportarBordero(1);

        Reserva segunda = reservas.reservar(1, 1, butacas("A2", TipoTarifa.GENERAL));
        pagos.cobrar(segunda.getId(), MedioPago.EFECTIVO, "");
        Bordero bordero = informes.exportarBordero(1);

        assertEquals(2, bordero.espectadores());
        assertTrue(leer(directorioInformes.resolve("bordero-funcion-1.txt")).contains("10000.00"));
    }

    // ---------- el informe financiero ----------

    @Test
    void elInformeSumaLasEntradasYElCandyDeLaFuncion() {
        Producto pochoclos = productos.agregar("Pochoclos",
                TipoProducto.POCHOCLOS, 3000);
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        candy.venderParaReserva(reserva.getId(), Map.of(pochoclos.getId(), 2),
                MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(1);

        assertEquals(5000.0, informe.bordero().recaudacionNeta(), 0.001);
        assertEquals(1, informe.comprasCandy());
        assertEquals(6000.0, informe.candy(), 0.001);
        assertEquals(11000.0, informe.total(), 0.001);
    }

    /**
     * La venta de mostrador no se atribuye a ninguna función: quien compra un balde puede
     * estar yendo a cualquiera de las funciones de esa hora, o a ninguna. Esa plata se
     * cuenta en el arqueo del día del candy, que acá se chequea justamente para dejar claro
     * que no se pierde: queda afuera del informe, no del sistema.
     */
    @Test
    void elCandyDeMostradorNoEntraEnElInformeDeNingunaFuncion() {
        Producto pochoclos = productos.agregar("Pochoclos",
                TipoProducto.POCHOCLOS, 3000);
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        candy.vender(null, Map.of(pochoclos.getId(), 1), MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(1);

        assertEquals(0, informe.comprasCandy());
        assertEquals(0, informe.candy(), 0.001);
        assertEquals(5000.0, informe.total(), 0.001);
        assertEquals(3000.0, candy.totalVendido(LocalDate.now()), 0.001);
    }

    /** El candy de la función de al lado tampoco: se atribuye por la reserva, no por el día. */
    @Test
    void elCandyDeOtraFuncionNoEntraEnEsteInforme() {
        Producto pochoclos = productos.agregar("Pochoclos",
                TipoProducto.POCHOCLOS, 3000);
        Reserva deLaOtra = reservas.reservar(2, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(deLaOtra.getId(), MedioPago.EFECTIVO, "");
        candy.venderParaReserva(deLaOtra.getId(), Map.of(pochoclos.getId(), 1),
                MedioPago.EFECTIVO, "");

        assertEquals(0, informes.informeDe(1).candy(), 0.001);
        assertEquals(3000.0, informes.informeDe(2).candy(), 0.001);
    }

    /** Sin candy, el informe es el neto del borderó y no otro número. */
    @Test
    void unaFuncionSinCandyRecaudaLoMismoQueSuBordero() {
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(1);

        assertEquals(informe.bordero().recaudacionNeta(), informe.total(), 0.001);
    }

    private static Map<String, TipoTarifa> butacas(String codigo, TipoTarifa tarifa) {
        return Map.of(codigo, tarifa);
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo leer " + archivo, e);
        }
    }
}
