package ar.uade.cine.service.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.ventas.PagoRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;

// El Observer de los comprobantes: se escriben después del commit, y solo si hubo commit.
class ComprobantesDeVentasTest extends PruebaDeIntegracion {

    private static final Path TICKETS = Path.of("target/comprobantes/tickets");

    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorPagos pagos;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private PlatformTransactionManager transacciones;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorClientes clientes;

    @BeforeEach
    void prepararEscenario() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        clientes.registrar("Andrei", "andrei@uade.edu.ar");
    }

    @Test
    void elTicketYElReciboSeEscribenCuandoLaVentaConfirma() {
        Reserva reserva = reservar();
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(List.of("recibo-" + pago.getId() + ".txt", "ticket-" + reserva.getId() + ".txt"),
                comprobantes());
    }

    // El aviso sale adentro de la transacción de afuera, que después se deshace: sin commit no hay ticket.
    @Test
    void siLaVentaSeDeshaceDespuesDeAvisarNoHayTicket() {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            reservar();
            estado.setRollbackOnly();
        });

        assertEquals(0, reservaRepository.count());
        assertEquals(List.of(), comprobantes());
    }

    // El caso que motivó el Observer. El @Version de Reserva salta recién en el commit, cuando otra
    // operación la canceló mientras se cobraba: antes quedaba el recibo de un cobro que no existió.
    @Test
    void unCobroQueChocaAlConfirmarNoDejaRecibo() {
        int id = reservar().getId();
        TransactionTemplate cobro = new TransactionTemplate(transacciones);
        TransactionTemplate cancelacion = new TransactionTemplate(transacciones);
        cancelacion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThrows(OptimisticLockingFailureException.class, () -> cobro.executeWithoutResult(estado -> {
            pagos.cobrar(id, MedioPago.EFECTIVO, "");
            cancelacion.executeWithoutResult(otra -> reservas.cancelar(id));
        }));

        assertEquals(EstadoReserva.CANCELADA, reservaRepository.findById(id).orElseThrow().getEstado());
        assertEquals(0, pagoRepository.count(), "el cobro se deshizo");
        assertEquals(List.of("ticket-" + id + ".txt"), comprobantes(), "sin recibo");
    }

    // Después del commit la venta ya existe: el archivo que falla se loguea y no la deshace.
    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void siElArchivoFallaLaVentaQuedaIgual(CapturedOutput log) throws IOException {
        Files.createDirectories(TICKETS.getParent());
        Files.writeString(TICKETS, "un archivo donde tendría que estar la carpeta");

        Reserva reserva = reservar();

        assertEquals(EstadoReserva.RESERVADA, reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
        assertTrue(log.getOut().contains("No se pudo emitir el ticket de la reserva " + reserva.getId()));
    }

    private Reserva reservar() {
        return reservas.reservar(1, 1, Map.of("A1", TipoTarifa.GENERAL), null);
    }

    private static List<String> comprobantes() {
        if (!Files.isDirectory(TICKETS)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(TICKETS)) {
            return archivos.map(archivo -> archivo.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
