package ar.uade.cine.service.informes;

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

import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.repository.AsientoDAO;
import ar.uade.cine.repository.ClienteDAO;
import ar.uade.cine.repository.CompraCandyDAO;
import ar.uade.cine.repository.FuncionDAO;
import ar.uade.cine.repository.PagoDAO;
import ar.uade.cine.repository.PeliculaDAO;
import ar.uade.cine.repository.ReservaDAO;
import ar.uade.cine.repository.SalaDAO;
import ar.uade.cine.repository.memoria.AsientoDAOMemoria;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.repository.memoria.ClienteDAOMemoria;
import ar.uade.cine.repository.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.repository.memoria.FuncionDAOMemoria;
import ar.uade.cine.repository.memoria.PagoDAOMemoria;
import ar.uade.cine.repository.memoria.PeliculaDAOMemoria;
import ar.uade.cine.repository.memoria.ProductoDAOMemoria;
import ar.uade.cine.repository.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.repository.memoria.PromocionDAOMemoria;
import ar.uade.cine.repository.memoria.ReservaDAOMemoria;
import ar.uade.cine.repository.memoria.SalaDAOMemoria;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.promociones.GestorPromociones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.CalculadoraPrecio;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.Ocupacion;
import ar.uade.cine.service.informes.GestorCaja;
import ar.uade.cine.model.dinero.Dinero;

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
    private GestorCaja caja;
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
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
        gestorFunciones.programar(1, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
        new GestorClientes(clienteDAO, reservaDAO, compraCandyDAO)
                .registrar("Andrei", "andrei@uade.edu.ar");

        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO, peliculaDAO,
                new GeneradorTicketTxt(tempDir.resolve("tickets")), new CalculadoraPrecio(),
                new Ocupacion(reservaDAO, funcionDAO, asientoDAO, new BloqueoButacasMemoria()));
        promociones = new GestorPromociones(new PromocionDAOMemoria());
        pagos = new GestorPagos(pagoDAO, reservaDAO, funcionDAO, promociones, new MercadoPagoEmulado(),
                new GeneradorReciboTxt(tempDir.resolve("tickets")));
        productos = new GestorProductos(new ProductoDAOMemoria());
        candy = new GestorCandy(compraCandyDAO, clienteDAO, reservaDAO,
                new GeneradorTicketCandyTxt(tempDir.resolve("tickets")), productos);
        informes = new GestorInformes(funcionDAO, peliculaDAO, salaDAO, reservaDAO, pagoDAO,
                compraCandyDAO, new GeneradorBorderoTxt(directorioInformes));
        caja = new GestorCaja(pagoDAO, reservaDAO, compraCandyDAO);
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
        assertEquals(Dinero.de(5000.0), bordero.recaudacionNeta());
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
        assertEquals(Dinero.de(10000.0), bordero.porTarifa().get(TipoTarifa.GENERAL).total());
        assertEquals(1, bordero.porTarifa().get(TipoTarifa.JUBILADO).cantidad());
        // La jubilada sale la mitad, y por eso el promedio por espectador no alcanza para
        // reconstruir este desglose.
        assertEquals(Dinero.de(2500.0), bordero.porTarifa().get(TipoTarifa.JUBILADO).total());
        assertEquals(Dinero.de(12500.0), bordero.recaudacionBruta());
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

        assertEquals(Dinero.de(5000.0), bordero.recaudacionBruta());
        assertEquals(Dinero.de(2500.0), bordero.descuentos());
        assertEquals(Dinero.de(2500.0), bordero.recaudacionNeta());
    }

    /** Una función que no vendió nada se declara igual: cero también es una declaración. */
    @Test
    void elBorderoDeUnaFuncionSinVentasDaEnCero() {
        Bordero bordero = informes.borderoDe(1);

        assertEquals(0, bordero.espectadores());
        assertEquals(Dinero.de(0), bordero.recaudacionBruta());
        assertEquals(Dinero.de(0), bordero.recaudacionNeta());
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
                TipoProducto.POCHOCLOS, Dinero.de(3000));
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        candy.venderParaReserva(reserva.getId(), Map.of(pochoclos.getId(), 2),
                MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(1);

        assertEquals(Dinero.de(5000.0), informe.bordero().recaudacionNeta());
        assertEquals(1, informe.comprasCandy());
        assertEquals(Dinero.de(6000.0), informe.candy());
        assertEquals(Dinero.de(11000.0), informe.total());
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
                TipoProducto.POCHOCLOS, Dinero.de(3000));
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        candy.vender(null, Map.of(pochoclos.getId(), 1), MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(1);

        assertEquals(0, informe.comprasCandy());
        assertEquals(Dinero.de(0), informe.candy());
        assertEquals(Dinero.de(5000.0), informe.total());
        assertEquals(Dinero.de(3000.0), caja.totalCandyDe(LocalDate.now()));
    }

    /** El candy de la función de al lado tampoco: se atribuye por la reserva, no por el día. */
    @Test
    void elCandyDeOtraFuncionNoEntraEnEsteInforme() {
        Producto pochoclos = productos.agregar("Pochoclos",
                TipoProducto.POCHOCLOS, Dinero.de(3000));
        Reserva deLaOtra = reservas.reservar(2, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(deLaOtra.getId(), MedioPago.EFECTIVO, "");
        candy.venderParaReserva(deLaOtra.getId(), Map.of(pochoclos.getId(), 1),
                MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(0), informes.informeDe(1).candy());
        assertEquals(Dinero.de(3000.0), informes.informeDe(2).candy());
    }

    /** Sin candy, el informe es el neto del borderó y no otro número. */
    @Test
    void unaFuncionSinCandyRecaudaLoMismoQueSuBordero() {
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(1);

        assertEquals(informe.bordero().recaudacionNeta(), informe.total());
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
