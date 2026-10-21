package ar.uade.cine.service.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.ventas.PagoRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.promociones.PoliticaPromociones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;

class ConciliacionPagosTest extends PruebaDeIntegracion {

    @Autowired
    private GestorReservas reservas;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private PoliticaPromociones politica;
    @Autowired
    private ApplicationEventPublisher eventos;
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

    private final PasarelaQueAprueba pasarela = new PasarelaQueAprueba();
    private GestorPagos pagos;
    private ConciliacionPagos conciliacion;

    @BeforeEach
    void prepararEscenario() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000)));
        clientes.identificar("Andrei", "andrei@uade.edu.ar");
        pagos = new GestorPagos(pagoRepository, reservaRepository, politica, pasarela, eventos, reloj);
        conciliacion = new ConciliacionPagos(pasarela, pagos, reloj);
    }

    // El comprador pagó en MP y no volvió al sitio: la reserva se cobra igual, antes de vencer.
    @Test
    void cobraUnCheckoutAprobadoSinQueElCompradorVuelva() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pasarela.aprobar(enTransaccion(() -> pagos.iniciarCheckout(reserva.getId(), MedioPago.QR)).id());

        assertEquals(1, enTransaccion(conciliacion::conciliar));

        assertEquals(EstadoReserva.PAGADA, reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
        assertEquals(Dinero.de(5000), pagoRepository.findByReservaId(reserva.getId()).orElseThrow().getMonto());
        assertEquals(reloj.ahora().minusMinutes(Reserva.MINUTOS_PARA_PAGAR), pasarela.desde);
    }

    @Test
    void loQueYaCobroLaVueltaNoSeCobraDeNuevo() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        String checkout = enTransaccion(() -> pagos.iniciarCheckout(reserva.getId(), MedioPago.QR)).id();
        pasarela.aprobar(checkout);
        enTransaccion(() -> pagos.retornarDeCheckout(checkout));

        assertEquals(0, enTransaccion(conciliacion::conciliar));
        assertEquals(0, enTransaccion(conciliacion::conciliar));

        assertEquals(1, pagoRepository.findByReservaIdIn(List.of(reserva.getId())).size());
    }

    // Uno que no se puede cobrar (acá, uno que MP no conoce) no frena a los demás.
    @Test
    void unCheckoutQueFallaNoFrenaALosDemas() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pasarela.aprobar("MP-0000000000");
        pasarela.aprobar(enTransaccion(() -> pagos.iniciarCheckout(reserva.getId(), MedioPago.QR)).id());

        assertEquals(1, enTransaccion(conciliacion::conciliar));
    }

    // GestorPagos armado a mano no tiene el proxy de @Transactional: sin esto la función LAZY no se carga.
    private <T> T enTransaccion(Supplier<T> accion) {
        return new TransactionTemplate(transacciones).execute(estado -> accion.get());
    }

    private static class PasarelaQueAprueba extends MercadoPagoEmulado {

        private final List<String> aprobados = new ArrayList<>();
        private LocalDateTime desde;

        void aprobar(String checkoutId) {
            aprobados.add(checkoutId);
        }

        @Override
        public List<String> aprobadosDesde(LocalDateTime desde) {
            this.desde = desde;
            return aprobados;
        }
    }

    private static Map<String, TipoTarifa> generales(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return butacas;
    }
}
