package ar.uade.cine.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.persistencia.archivo.GeneradorTicketTxt;
import ar.uade.cine.persistencia.archivo.ReservaDAOTxt;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PagoDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;

/** R5: solo se cobra una reserva en estado RESERVADA, y una sola vez. */
class GestorPagosTest {

    @TempDir
    Path tempDir;

    private GestorReservas reservas;
    private GestorPagos pagos;
    private ReservaDAO reservaDAO;

    /** Sala 2D de 10 butacas, función a $5000, un cliente. */
    @BeforeEach
    void prepararEscenario() {
        PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
        SalaDAO salaDAO = new SalaDAOMemoria();
        AsientoDAO asientoDAO = new AsientoDAOMemoria();
        FuncionDAO funcionDAO = new FuncionDAOMemoria();
        ClienteDAO clienteDAO = new ClienteDAOMemoria();
        PagoDAO pagoDAO = new PagoDAOMemoria();
        reservaDAO = new ReservaDAOTxt(tempDir.resolve("reservas.txt"));

        new GestorCartelera(peliculaDAO, funcionDAO)
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        new GestorSalas(salaDAO, asientoDAO, funcionDAO).agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO)
                .programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, 5000);
        new GestorClientes(clienteDAO, reservaDAO, new CompraCandyDAOMemoria()).registrar("Andrei", "andrei@uade.edu.ar");

        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO, peliculaDAO,
                new GeneradorTicketTxt(tempDir.resolve("tickets")));
        pagos = new GestorPagos(pagoDAO, reservaDAO);
    }

    @Test
    void elMontoSaleDeLaReservaYNoDeQuienCobra() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1", "A2"));

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(10000.0, pago.getMonto(), 0.001);
        assertEquals(reserva.getTotal(), pago.getMonto(), 0.001);
    }

    @Test
    void cobrarDejaLaReservaPagada() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1"));
        pagos.cobrar(reserva.getId(), MedioPago.DEBITO, "AUT-123");

        assertEquals(EstadoReserva.PAGADA,
                reservaDAO.buscarPorId(reserva.getId()).orElseThrow().getEstado());
    }

    @Test
    void noSeCobraDosVecesLaMismaReserva() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1"));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void noSeCobraUnaReservaCancelada() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1"));
        reservas.cancelar(reserva.getId());

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void losMediosElectronicosExigenCodigoDeAutorizacion() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1"));

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.CREDITO, "  "));
    }

    @Test
    void elEfectivoNoNecesitaCodigo() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1"));
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals("", pago.getCodigoAutorizacion());
    }

    @Test
    void elArqueoDeBoleteriaSumaLoCobradoEnElDia() {
        Reserva primera = reservas.reservar(1, 1, List.of("A1", "A2"));
        Reserva segunda = reservas.reservar(1, 1, List.of("B1"));
        pagos.cobrar(primera.getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(segunda.getId(), MedioPago.QR, "QR-99");

        assertEquals(2, pagos.listarDelDia(LocalDate.now()).size());
        assertEquals(15000.0, pagos.totalCobrado(LocalDate.now()), 0.001);
        assertTrue(pagos.buscarPorReserva(primera.getId()).isPresent());
    }
}
