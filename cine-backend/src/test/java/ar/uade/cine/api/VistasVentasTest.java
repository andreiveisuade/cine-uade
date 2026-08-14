package ar.uade.cine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.Aplicacion;
import ar.uade.cine.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.dto.ventas.EntradaVistaDTO;
import ar.uade.cine.dto.ventas.PagoVistaDTO;
import ar.uade.cine.dto.ventas.ReservaVistaDTO;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.BloqueoButacasMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.EmpleadoDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PagoDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProductoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PromocionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;

/**
 * La reserva viaja como un ticket completo —qué película, en qué sala, a qué hora, de
 * quién y si ya se pagó— porque eso es lo que el front muestra en una sola pantalla.
 *
 * <p>Los dos campos que más importan acá son los que usa la puerta: el código del QR y
 * cuándo se ingresó. El segundo viaja en null mientras nadie entró, y esa distinción es
 * la que le permite al acomodador saber si el ticket ya se usó.
 */
class VistasVentasTest {

    @TempDir
    Path tempDir;

    private Aplicacion aplicacion;
    private VistasVentas vistas;
    private Cliente cliente;

    @BeforeEach
    void prepararEscenario() {
        aplicacion = new Aplicacion(
                new PeliculaDAOMemoria(), new SalaDAOMemoria(), new AsientoDAOMemoria(),
                new FuncionDAOMemoria(), new ClienteDAOMemoria(), new EmpleadoDAOMemoria(),
                new ReservaDAOMemoria(), new PagoDAOMemoria(),
                new PromocionDAOMemoria(), new ProgramacionDAOMemoria(), new ProductoDAOMemoria(),
                new CompraCandyDAOMemoria(),
                new BloqueoButacasMemoria(),
                new GeneradorTicketTxt(tempDir.resolve("tickets")),
                new GeneradorTicketCandyTxt(tempDir.resolve("tickets")),
                new GeneradorReciboTxt(tempDir.resolve("tickets")),
                new GeneradorBorderoTxt(tempDir.resolve("informes")),
                new MercadoPagoEmulado());

        VistasSalas vistasSalas = new VistasSalas(aplicacion.getSalas(),
                aplicacion.getCalculadoraPrecio());
        VistasCartelera vistasCartelera = new VistasCartelera(aplicacion.getCartelera(),
                aplicacion.getSalas(), aplicacion.getOcupacion(),
                aplicacion.getCalculadoraPrecio(), vistasSalas);
        vistas = new VistasVentas(aplicacion.getFunciones(), aplicacion.getSalas(),
                aplicacion.getCartelera(), aplicacion.getClientes(), aplicacion.getPagos(),
                aplicacion.getReservas(), vistasCartelera, vistasSalas, new VistasUsuarios());

        aplicacion.getCartelera().agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Sala sala = aplicacion.getSalas().agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        aplicacion.getFunciones().programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
        cliente = aplicacion.getClientes().identificar("Andrei", "andrei@uade.edu.ar");
    }

    @Test
    void laReservaTraeTodoLoQueImprimeElTicket() {
        ReservaVistaDTO vista = vistas.reserva(reservar("A1", "A2"));

        assertEquals("Matrix", vista.pelicula().titulo());
        assertEquals("Sala 1", vista.sala().nombre());
        assertEquals("2026-08-20T20:00:00", vista.funcion().inicio());
        assertEquals("Andrei", vista.cliente().nombre());
        assertEquals(2, vista.cantidadEntradas());
        assertEquals(10000.0, vista.total(), 0.001);
        assertEquals("RESERVADA", vista.estado());
    }

    /** El email del cliente viaja: es con lo que se identifica al comprar sin registrarse. */
    @Test
    void elClienteViajaSinDatosDeMas() {
        ReservaVistaDTO vista = vistas.reserva(reservar("A1"));

        assertEquals(cliente.getId(), vista.cliente().id());
        assertEquals("andrei@uade.edu.ar", vista.cliente().email());
    }

    /**
     * La tarifa viaja por entrada para que el acomodador sepa a quién pedirle el carnet:
     * un jubilado y un menor pagaron menos y tienen que poder acreditarlo.
     */
    @Test
    void cadaEntradaDiceConQueTarifaSeVendio() {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        butacas.put("A1", TipoTarifa.GENERAL);
        butacas.put("A2", TipoTarifa.JUBILADO);

        ReservaVistaDTO vista = vistas.reserva(aplicacion.getReservas()
                .reservar(1, cliente.getId(), butacas));

        assertEquals("GENERAL", entrada(vista, "A1").tarifa());
        assertEquals("JUBILADO", entrada(vista, "A2").tarifa());
        assertTrue(entrada(vista, "A2").precio() < entrada(vista, "A1").precio());
    }

    // ---------- lo que necesita la puerta ----------

    /** El código del QR no es el id: es lo que se escanea y no se puede adivinar. */
    @Test
    void laReservaViajaConElCodigoDelQr() {
        Reserva reserva = reservar("A1");

        ReservaVistaDTO vista = vistas.reserva(reserva);

        assertEquals(reserva.getCodigo(), vista.codigo());
        assertTrue(!vista.codigo().equals(String.valueOf(vista.id())));
    }

    /**
     * Mientras nadie entró, ingresadaEn viaja en null y el campo ni aparece en el JSON.
     * Es distinto de una fecha vacía: el escáner lee "todavía no se usó".
     */
    @Test
    void ingresadaEnEstaEnNullHastaQueAlguienEntra() {
        Reserva reserva = reservar("A1");
        assertNull(vistas.reserva(reserva).ingresadaEn());

        aplicacion.getPagos().cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        Reserva ingresada = aplicacion.getReservas().registrarIngreso(reserva.getCodigo());

        assertNotNull(vistas.reserva(ingresada).ingresadaEn());
        assertEquals(19, vistas.reserva(ingresada).ingresadaEn().length(),
                "tiene que usar el mismo formato que el resto de las fechas");
    }

    // ---------- el pago ----------

    @Test
    void sinCobrarLaReservaViajaSinPago() {
        assertNull(vistas.reserva(reservar("A1")).pago());
    }

    @Test
    void cobradaLaReservaTraeSuPagoYCambiaDeEstado() {
        Reserva reserva = reservar("A1");
        aplicacion.getPagos().cobrar(reserva.getId(), MedioPago.CREDITO, "AUTH-123");

        ReservaVistaDTO vista = vistas.reserva(aplicacion.getReservas()
                .buscar(reserva.getId()).orElseThrow());

        assertEquals("PAGADA", vista.estado());
        assertEquals("CREDITO", vista.pago().medio());
        assertEquals("AUTH-123", vista.pago().codigoAutorizacion());
        assertEquals(5000.0, vista.pago().monto(), 0.001);
    }

    /**
     * El pago embebido en la reserva no repite la película ni el cliente: ya vienen en la
     * reserva que lo contiene. Mandarlos otra vez sería duplicar el mismo dato en un JSON
     * que el front ya sabe leer de un solo lugar.
     */
    @Test
    void elPagoEmbebidoNoRepiteLoQueYaTraeLaReserva() {
        Reserva reserva = reservar("A1");
        Pago pago = aplicacion.getPagos().cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        PagoVistaDTO vista = vistas.pago(pago);

        assertNull(vista.pelicula());
        assertNull(vista.cliente());
        assertNull(vista.entradas());
    }

    /**
     * El arqueo es la vista del encargado: muestra qué se cobró, no solo cuánto. Ahí sí
     * cada pago tiene que traer la película y el cliente, porque la reserva no está
     * alrededor para consultarla.
     */
    @Test
    void elPagoDelArqueoSiTraeQueSeVendioYAQuien() {
        Reserva reserva = reservar("A1", "A2");
        Pago pago = aplicacion.getPagos().cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        PagoVistaDTO vista = vistas.pagoDeArqueo(pago);

        assertEquals("Matrix", vista.pelicula().titulo());
        assertEquals("Andrei", vista.cliente().nombre());
        assertEquals(2, vista.entradas());
        assertEquals(10000.0, vista.monto(), 0.001);
    }

    private Reserva reservar(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return aplicacion.getReservas().reservar(1, cliente.getId(), butacas);
    }

    private static EntradaVistaDTO entrada(ReservaVistaDTO vista, String codigo) {
        return vista.entradas().stream()
                .filter(e -> e.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La reserva no tiene la butaca " + codigo));
    }
}
