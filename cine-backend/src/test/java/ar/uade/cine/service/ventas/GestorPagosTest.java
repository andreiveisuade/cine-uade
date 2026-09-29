package ar.uade.cine.service.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import ar.uade.cine.PruebaDeIntegracion;

import ar.uade.cine.repository.ventas.PagoRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.infrastructure.comprobantes.GeneradorRecibo;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.informes.Arqueo;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.model.promociones.CondicionesPromocion;
import ar.uade.cine.service.promociones.GestorPromociones;
import ar.uade.cine.service.promociones.PoliticaPromociones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.informes.GestorCaja;
import ar.uade.cine.model.dinero.Dinero;

class GestorPagosTest extends PruebaDeIntegracion {

    private static final Path TICKETS = Path.of("target/comprobantes/tickets");

    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorPagos pagos;
    @Autowired
    private GestorCaja caja;
    @Autowired
    private GestorPromociones promociones;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private PoliticaPromociones politica;
    @Autowired
    private GeneradorRecibo generadorRecibo;
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
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
        clientes.registrar("Andrei", "andrei@uade.edu.ar");
    }

    @Test
    void elMontoSaleDeLaReservaYNoDeQuienCobra() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2"), null);

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(10000.0), pago.getMonto());
        assertEquals(reserva.getTotal(), pago.getMonto());
    }

    @Test
    void cobrarDejaLaReservaPagada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pagos.cobrar(reserva.getId(), MedioPago.DEBITO, "AUT-123");

        assertEquals(EstadoReserva.PAGADA,
                reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
    }

    @Test
    void noSeCobraDosVecesLaMismaReserva() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void noSeCobraUnaReservaCancelada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        reservas.cancelar(reserva.getId());

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    // Las dos transacciones de cobrar y cancelar a la vez: cada una lee la reserva todavía
    // RESERVADA, pasa su chequeo y escribe. Sin @Version ganaba la última; ahora la segunda
    // escribe sobre una versión vieja y falla, y la reserva queda como la dejó la primera.
    @Test
    void cobrarYCancelarALaVezLaMismaReservaNoTerminanLasDos() {
        int id = reservas.reservar(1, 1, generales("A1"), null).getId();
        Reserva laQueCobra = reservaRepository.findById(id).orElseThrow();
        Reserva laQueCancela = reservaRepository.findById(id).orElseThrow();

        laQueCobra.pagar();
        reservaRepository.save(laQueCobra);
        laQueCancela.cancelar();

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> reservaRepository.save(laQueCancela));
        assertEquals(EstadoReserva.PAGADA, reservaRepository.findById(id).orElseThrow().getEstado());
    }

    @Test
    void losMediosElectronicosExigenCodigoDeAutorizacion() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.CREDITO, "  "));

        assertEquals("Falta el código de autorización del pago con crédito", error.getMessage());
    }

    @Test
    void elEfectivoNoNecesitaCodigo() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals("", pago.getCodigoAutorizacion());
    }

    @Test
    void elArqueoDeBoleteriaSumaLoCobradoEnElDia() {
        Reserva primera = reservas.reservar(1, 1, generales("A1", "A2"), null);
        Reserva segunda = reservas.reservar(1, 1, generales("B1"), null);
        pagos.cobrar(primera.getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(segunda.getId(), MedioPago.QR, "QR-99");

        Arqueo arqueo = caja.arqueoDe(reloj.hoy());
        assertEquals(2, arqueo.pagos().size());
        assertEquals(Dinero.de(15000.0), arqueo.total());
        assertTrue(pagos.buscarPorReserva(primera.getId()).isPresent());
    }


    @Test
    void elPagoGuardaSubtotalDescuentoYPromocion() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2"), null);
        Promocion promo = promociones.crearNxM("2x1", 2, 1,
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()));

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(reserva.getTotal(), pago.getSubtotal());
        assertEquals(promo.getId(), pago.getPromocionId());
        assertEquals(pago.getSubtotal().menos(pago.getDescuento()), pago.getMonto());
        assertTrue(pago.getDescuento().esMayorQue(Dinero.CERO));
    }

    @Test
    void sinPromocionAplicableElMontoEsElSubtotal() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertNull(pago.getPromocionId());
        assertEquals(Dinero.de(0), pago.getDescuento());
        assertEquals(pago.getSubtotal(), pago.getMonto());
    }

    @Test
    void elDescuentoBancarioSoloEntraSiSePagaConEseMedio() {
        promociones.crearMontoFijo("Banco", Dinero.de(1000),
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of(MedioPago.CREDITO)));

        Reserva enEfectivo = reservas.reservar(1, 1, generales("A1"), null);
        Reserva conTarjeta = reservas.reservar(1, 1, generales("A2"), null);

        assertEquals(Dinero.de(0), pagos.cobrar(enEfectivo.getId(), MedioPago.EFECTIVO, "").getDescuento());
        assertEquals(Dinero.de(1000), pagos.cobrar(conTarjeta.getId(), MedioPago.CREDITO, "AUT-1").getDescuento());
    }

    @Test
    void elArqueoCuentaElMontoCobradoYNoElSubtotal() {
        promociones.crearPorcentaje("50 off", 50.0,
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()));
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Dinero arqueo = caja.arqueoDe(reloj.hoy()).total();

        assertEquals(pago.getMonto(), arqueo);
        assertTrue(pago.getSubtotal().esMayorQue(arqueo));
    }

    @Test
    void elArqueoResumeTotalEntradasYRepartoPorMedio() {
        Reserva primera = reservas.reservar(1, 1, generales("A1", "A2"), null);
        Reserva segunda = reservas.reservar(1, 1, generales("B1"), null);
        pagos.cobrar(primera.getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(segunda.getId(), MedioPago.QR, "QR-99");

        Arqueo arqueo = caja.arqueoDe(reloj.hoy());

        assertEquals(Dinero.de(15000.0), arqueo.total());
        assertEquals(3, arqueo.entradas());
        assertEquals(2, arqueo.pagos().size());
        assertEquals(1, arqueo.porMedio().get(MedioPago.EFECTIVO).cantidad());
        assertEquals(Dinero.de(10000.0), arqueo.porMedio().get(MedioPago.EFECTIVO).total());
        assertEquals(Dinero.de(5000.0), arqueo.porMedio().get(MedioPago.QR).total());
    }

    @Test
    void elArqueoDeUnDiaSinCobrosDaEnCero() {
        Arqueo arqueo = caja.arqueoDe(reloj.hoy().minusDays(1));

        assertEquals(Dinero.de(0), arqueo.total());
        assertEquals(0, arqueo.entradas());
        assertTrue(arqueo.pagos().isEmpty());
        assertTrue(arqueo.porMedio().isEmpty());
    }

    @Test
    void elRepartoPorMedioCuentaElMontoConDescuento() {
        promociones.crearPorcentaje("50 off", 50.0,
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()));
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Arqueo arqueo = caja.arqueoDe(reloj.hoy());

        assertEquals(pago.getMonto(), arqueo.total());
        assertEquals(pago.getMonto(), arqueo.porMedio().get(MedioPago.EFECTIVO).total());
        assertTrue(pago.getSubtotal().esMayorQue(arqueo.total()));
    }

    @Test
    void elCobroEnEfectivoImprimeElReciboDeCaja() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Path recibo = TICKETS.resolve("recibo-" + pago.getId() + ".txt");

        assertTrue(Files.exists(recibo));
        assertTrue(leer(recibo).contains("EFECTIVO"));
    }

    @Test
    void elCobroElectronicoNoImprimeReciboDeCaja() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.CREDITO, "AUT-123");

        assertFalse(Files.exists(TICKETS.resolve("recibo-" + pago.getId() + ".txt")));
    }

    @Test
    void elReciboMuestraElDescuentoQueSeAplicoAlCobrar() {
        promociones.crearPorcentaje("50 off", 50.0,
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()));
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        String recibo = leer(TICKETS.resolve("recibo-" + pago.getId() + ".txt"));

        assertTrue(recibo.contains("Descuento"));
        assertTrue(recibo.contains("2500.00"));
    }

    @Test
    void elCheckoutViajaConElLinkYElQrDeLaPasarela() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR);

        assertEquals(reserva.getId(), checkout.reservaId());
        assertEquals(MedioPago.QR, checkout.medio());
        assertEquals(Dinero.de(5000.0), checkout.monto());
        assertFalse(checkout.urlPago().isBlank());
        assertFalse(checkout.codigoQr().isBlank());
    }

    @Test
    void elMontoDelCheckoutYaTraeElDescuentoAplicado() {
        promociones.crearPorcentaje("50 off", 50.0,
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()));
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        assertEquals(Dinero.de(2500.0), pagos.iniciarCheckout(reserva.getId(), MedioPago.QR).monto());
    }

    @Test
    void elEfectivoNoAbreCheckout() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        assertThrows(IllegalArgumentException.class,
                () -> pagos.iniciarCheckout(reserva.getId(), MedioPago.EFECTIVO));
    }

    @Test
    void noSeAbreCheckoutDeUnaReservaYaPagada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertThrows(IllegalArgumentException.class,
                () -> pagos.iniciarCheckout(reserva.getId(), MedioPago.QR));
    }

    @Test
    void confirmarElCheckoutCobraConElCodigoQueDevolvioLaPasarela() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2"), null);
        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR);

        Pago pago = pagos.confirmarCheckout(checkout.id());

        assertEquals(reserva.getId(), pago.getReservaId());
        assertEquals(MedioPago.QR, pago.getMedio());
        assertEquals(Dinero.de(10000.0), pago.getMonto());
        assertFalse(pago.getCodigoAutorizacion().isBlank());
        assertEquals(EstadoReserva.PAGADA,
                reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
    }

    @Test
    void noSeConfirmaUnCheckoutQueNoExiste() {
        assertThrows(IllegalArgumentException.class, () -> pagos.confirmarCheckout("MP-0000000000"));
    }

    @Test
    void confirmarDosVecesElMismoCheckoutNoCobraDeNuevo() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR);
        pagos.confirmarCheckout(checkout.id());

        assertThrows(IllegalArgumentException.class, () -> pagos.confirmarCheckout(checkout.id()));
        assertEquals(1, caja.arqueoDe(reloj.hoy()).pagos().size());
    }

    // Autorizar es cobrarle al cliente y no hay devolución (R13): la reserva que dejó de ser
    // cobrable se rechaza antes de llegar a la pasarela, igual que al abrir el checkout.
    // El checkout se abre directo en la pasarela: se abrió cuando todavía se podía cobrar.
    @Test
    void noSeAutorizaEnLaPasarelaElCheckoutDeUnaReservaQueYaNoSePuedeCobrar() {
        PasarelaQueCuenta pasarela = new PasarelaQueCuenta();
        GestorPagos conEsaPasarela = new GestorPagos(pagoRepository, reservaRepository, politica,
                pasarela, generadorRecibo, reloj);
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        String checkout = pasarela.crear(reserva.getId(), MedioPago.QR, reserva.getTotal()).id();
        reservas.cancelar(reserva.getId());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> conEsaPasarela.confirmarCheckout(checkout));

        assertEquals("La reserva está cancelada: no se puede cobrar", error.getMessage());
        assertEquals(0, pasarela.autorizaciones);
    }

    private static class PasarelaQueCuenta extends MercadoPagoEmulado {

        private int autorizaciones;

        @Override
        public String autorizar(Checkout checkout) {
            autorizaciones++;
            return super.autorizar(checkout);
        }
    }

    private static Map<String, TipoTarifa> generales(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return butacas;
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + archivo, e);
        }
    }
}
