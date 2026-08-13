package ar.uade.cine;

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

import ar.uade.cine.interfaces.AsientoDAO;
import ar.uade.cine.interfaces.ClienteDAO;
import ar.uade.cine.interfaces.FuncionDAO;
import ar.uade.cine.interfaces.Pago;
import ar.uade.cine.interfaces.PagoDAO;
import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.interfaces.ReservaDAO;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.Clasificacion;
import ar.uade.cine.modelo.EstadoReserva;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.modelo.Idioma;
import ar.uade.cine.modelo.MedioPago;
import ar.uade.cine.modelo.Proyeccion;
import ar.uade.cine.modelo.TipoSala;
import ar.uade.cine.persistencia.AsientoDAOMemoria;
import ar.uade.cine.persistencia.ClienteDAOMemoria;
import ar.uade.cine.persistencia.FuncionDAOMemoria;
import ar.uade.cine.persistencia.GeneradorTicketTxt;
import ar.uade.cine.persistencia.PagoDAOMemoria;
import ar.uade.cine.persistencia.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.ReservaDAOTxt;
import ar.uade.cine.persistencia.SalaDAOMemoria;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;

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

        new GestorCartelera(peliculaDAO).agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        new GestorSalas(salaDAO, asientoDAO).agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        new GestorFunciones(funcionDAO, peliculaDAO, salaDAO)
                .programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0), Idioma.DOBLADA, Proyeccion.DOS_D, 5000);
        new GestorClientes(clienteDAO).registrar("Andrei", "andrei@uade.edu.ar");

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
    void elArqueoSumaLoCobradoEnElDia() {
        Reserva primera = reservas.reservar(1, 1, List.of("A1", "A2"));
        Reserva segunda = reservas.reservar(1, 1, List.of("B1"));
        pagos.cobrar(primera.getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(segunda.getId(), MedioPago.QR, "QR-99");

        assertEquals(2, pagos.listarDelDia(LocalDate.now()).size());
        assertEquals(15000.0, pagos.totalCobrado(LocalDate.now()), 0.001);
        assertTrue(pagos.buscarPorReserva(primera.getId()).isPresent());
    }
}
