package ar.uade.cine.service.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ar.uade.cine.model.imagenes.ImagenUrl;
import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.Rechazo;

import ar.uade.cine.repository.ventas.PagoRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.informes.Arqueo;
import ar.uade.cine.model.promociones.CondicionesPromocion;
import ar.uade.cine.model.promociones.ParametrosPromocion;
import ar.uade.cine.model.promociones.TipoPromocion;
import ar.uade.cine.service.promociones.GestorPromociones;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.EstadoCompraCandy;
import ar.uade.cine.model.candy.TipoProducto;
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
    private GestorCandy candy;
    @Autowired
    private GestorProductos carta;
    @Autowired
    private PlatformTransactionManager transacciones;
    @Autowired
    private ApplicationEventPublisher eventos;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorClientes clientes;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararEscenario() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000)));
        clientes.identificar("Andrei", "andrei@uade.edu.ar");
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

        rechaza("La reserva está pagada: no se puede cobrar",
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    // Con la reserva todavía RESERVADA, la guarda es lo único que frena un segundo pago (el UNIQUE daría un 500).
    @Test
    @DisplayName("R5: no se registra un segundo pago de la misma reserva")
    void aunqueElEstadoNoLoFreneLaBaseNoAceptaUnSegundoPago() {
        // R5: el estado PAGADA corta antes; si algo lo saltea, el UNIQUE de pago.reserva_id es la última línea.
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        jdbc.update("INSERT INTO pago (reserva_id, subtotal, monto, medio, fecha)"
                + " VALUES (?, 5000, 5000, 'EFECTIVO', ?)",
                reserva.getId(), LocalDateTime.of(2026, 8, 14, 10, 0));

        assertThrows(DataIntegrityViolationException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void noSeCobraUnaReservaCancelada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        reservas.cancelar(reserva.getId());

        rechaza("La reserva está cancelada: no se puede cobrar",
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    // Cobrar y cancelar a la vez: ambas leen RESERVADA y escriben; @Version hace fallar a la segunda.
    @Test
    void cobrarYCancelarALaVezLaMismaReservaNoTerminanLasDos() {
        int id = reservas.reservar(1, 1, generales("A1"), null).getId();
        Reserva laQueCobra = reservaRepository.findById(id).orElseThrow();
        Reserva laQueCancela = reservaRepository.findById(id).orElseThrow();

        laQueCobra.pagar();
        reservaRepository.save(laQueCobra);
        laQueCancela.cancelar(reloj.ahora());

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> reservaRepository.save(laQueCancela));
        assertEquals(EstadoReserva.PAGADA, reservaRepository.findById(id).orElseThrow().getEstado());
    }

    @Test
    void losMediosElectronicosExigenCodigoDeAutorizacion() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        Rechazo error = assertThrows(Rechazo.class,
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
    void elPagoGuardaSubtotalDescuentoYPromocion() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2"), null);
        Promocion promo = promociones.crear(TipoPromocion.NXM, "2x1", ParametrosPromocion.deNxM(2, 1),
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()), ImagenUrl.NINGUNA);

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(reserva.getTotal(), pago.getSubtotal());
        assertEquals(promo.getId(), pago.getPromocionId());
        assertEquals(Dinero.de(5000), pago.getDescuento(), "el 2x1 de dos entradas de 5000 regala una");
        assertEquals(Dinero.de(5000), pago.getMonto());
    }

    @Test
    @DisplayName("R16: al cobrar, la promoción corre solo sobre las entradas de tarifa general")
    void alCobrarLaPromocionCorreSoloSobreLaTarifaGeneral() {
        promociones.crear(TipoPromocion.PORCENTAJE, "50 off", ParametrosPromocion.dePorcentaje(50.0),
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()), ImagenUrl.NINGUNA);
        Map<String, TipoTarifa> mixta = new LinkedHashMap<>();
        mixta.put("A1", TipoTarifa.GENERAL);
        mixta.put("A2", TipoTarifa.JUBILADO);
        Reserva reserva = reservas.reservar(1, 1, mixta, null);

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(7500), pago.getSubtotal());
        assertEquals(Dinero.de(2500), pago.getDescuento(), "el 50 % de la general, no del total");
        assertEquals(Dinero.de(5000), pago.getMonto());
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
        promociones.crear(TipoPromocion.MONTO_FIJO, "Banco", ParametrosPromocion.deMonto(1000.0),
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of(MedioPago.CREDITO)), ImagenUrl.NINGUNA);

        Reserva enEfectivo = reservas.reservar(1, 1, generales("A1"), null);
        Reserva conTarjeta = reservas.reservar(1, 1, generales("A2"), null);

        assertEquals(Dinero.de(0), pagos.cobrar(enEfectivo.getId(), MedioPago.EFECTIVO, "").getDescuento());
        assertEquals(Dinero.de(1000), pagos.cobrar(conTarjeta.getId(), MedioPago.CREDITO, "AUT-1").getDescuento());
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
        promociones.crear(TipoPromocion.PORCENTAJE, "50 off", ParametrosPromocion.dePorcentaje(50.0),
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()), ImagenUrl.NINGUNA);
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
        assertTrue(leer(recibo).contains("Efectivo"));
    }

    @Test
    void elCobroElectronicoNoImprimeReciboDeCaja() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.CREDITO, "AUT-123");

        assertFalse(Files.exists(TICKETS.resolve("recibo-" + pago.getId() + ".txt")));
    }

    @Test
    void elReciboMuestraElDescuentoQueSeAplicoAlCobrar() {
        promociones.crear(TipoPromocion.PORCENTAJE, "50 off", ParametrosPromocion.dePorcentaje(50.0),
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()), ImagenUrl.NINGUNA);
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
        promociones.crear(TipoPromocion.PORCENTAJE, "50 off", ParametrosPromocion.dePorcentaje(50.0),
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()), ImagenUrl.NINGUNA);
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        assertEquals(Dinero.de(2500.0), pagos.iniciarCheckout(reserva.getId(), MedioPago.QR).monto());
    }

    @Test
    void elCheckoutVenceCuandoVenceLaReserva() {
        PasarelaQueCuenta pasarela = new PasarelaQueCuenta();
        GestorPagos conEsaPasarela = new GestorPagos(pagoRepository, reservaRepository, politica,
                pasarela, candy, eventos, reloj);
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        new TransactionTemplate(transacciones)
                .executeWithoutResult(estado -> conEsaPasarela.iniciarCheckout(reserva.getId(), MedioPago.QR));

        assertEquals(reserva.getCreadaEn().plusMinutes(Reserva.MINUTOS_PARA_PAGAR), pasarela.vence);
    }

    @Test
    void elEfectivoNoAbreCheckout() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        rechaza("El pago con efectivo no va por checkout: se cobra en la caja del cine",
                () -> pagos.iniciarCheckout(reserva.getId(), MedioPago.EFECTIVO));
    }

    @Test
    void noSeAbreCheckoutDeUnaReservaYaPagada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        rechaza("La reserva está pagada: no se puede cobrar",
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
        rechaza("No existe el checkout MP-0000000000", () -> pagos.confirmarCheckout("MP-0000000000"));
    }

    @Test
    void confirmarDosVecesElMismoCheckoutNoCobraDeNuevo() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR);
        pagos.confirmarCheckout(checkout.id());

        rechaza("La reserva está pagada: no se puede cobrar", () -> pagos.confirmarCheckout(checkout.id()));
        assertEquals(1, caja.arqueoDe(reloj.hoy()).pagos().size());
    }

    // Autorizar es cobrar sin devolución (R13): la reserva ya no cobrable se rechaza antes de la pasarela,
    // aunque el checkout se haya abierto cuando todavía se podía.
    @Test
    void noSeAutorizaEnLaPasarelaElCheckoutDeUnaReservaQueYaNoSePuedeCobrar() {
        PasarelaQueCuenta pasarela = new PasarelaQueCuenta();
        GestorPagos conEsaPasarela = new GestorPagos(pagoRepository, reservaRepository, politica,
                pasarela, candy, eventos, reloj);
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        String checkout = pasarela.crear(reserva.getId(), MedioPago.QR, reserva.getTotal(), Dinero.CERO, reloj.ahora()).id();
        reservas.cancelar(reserva.getId());

        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                () -> conEsaPasarela.confirmarCheckout(checkout));

        assertEquals("La reserva está cancelada: no se puede cobrar", error.getMessage());
        assertEquals(0, pasarela.autorizaciones);
    }

    @Test
    void volverDeLaPasarelaConElPagoAprobadoCobraLaReserva() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        String checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR).id();

        RetornoCheckout retorno = pagos.retornarDeCheckout(checkout, null);

        assertEquals(new RetornoCheckout(reserva.getCodigo(), null), retorno);
        assertEquals(Dinero.de(5000.0), pagoRepository.findByReservaId(reserva.getId()).orElseThrow().getMonto());
    }

    @Test
    void volverDeLaPasarelaSinPagoAprobadoNoCobraYDiceElMotivo() {
        PasarelaQueNoAprueba pasarela = new PasarelaQueNoAprueba();
        GestorPagos conEsaPasarela = new GestorPagos(pagoRepository, reservaRepository, politica,
                pasarela, candy, eventos, reloj);
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        String checkout = pasarela.crear(reserva.getId(), MedioPago.QR, reserva.getTotal(), Dinero.CERO, reloj.ahora()).id();

        // Armado a mano no tiene el proxy de @Transactional: sin esto la función LAZY no se carga.
        RetornoCheckout retorno = new TransactionTemplate(transacciones)
                .execute(estado -> conEsaPasarela.retornarDeCheckout(checkout, null));

        assertEquals(new RetornoCheckout(reserva.getCodigo(), "Mercado Pago rechazó el pago"), retorno);
        assertFalse(pagoRepository.existsByReservaId(reserva.getId()));
    }

    @Test
    void volverDeLaPasarelaConElPagoRechazadoNoCobraYDiceElMotivo() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        String checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR).id();

        RetornoCheckout retorno = pagos.retornarDeCheckout(checkout, "rejected");

        assertEquals(new RetornoCheckout(reserva.getCodigo(),
                "Mercado Pago rechazó el pago: probá de nuevo con otro medio"), retorno);
        assertFalse(pagoRepository.existsByReservaId(reserva.getId()));
    }

    // El status lo escribe cualquiera en la URL, también la pantalla emulada: approved solo no cobra.
    @Test
    void elStatusApprovedDeLaVueltaNoCobraLoQueLaPasarelaNoAutoriza() {
        PasarelaQueNoAprueba pasarela = new PasarelaQueNoAprueba();
        GestorPagos conEsaPasarela = new GestorPagos(pagoRepository, reservaRepository, politica,
                pasarela, candy, eventos, reloj);
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        String checkout = pasarela.crear(reserva.getId(), MedioPago.QR, reserva.getTotal(), Dinero.CERO, reloj.ahora()).id();

        RetornoCheckout retorno = new TransactionTemplate(transacciones)
                .execute(estado -> conEsaPasarela.retornarDeCheckout(checkout, "approved"));

        assertEquals(new RetornoCheckout(reserva.getCodigo(), "Mercado Pago rechazó el pago"), retorno);
        assertFalse(pagoRepository.existsByReservaId(reserva.getId()));
    }

    // Recargar la página de vuelta no es un segundo cobro ni un error para el comprador.
    @Test
    void volverDosVecesDeLaPasarelaCobraUnaSola() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        String checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR).id();
        pagos.retornarDeCheckout(checkout, null);

        RetornoCheckout otraVez = pagos.retornarDeCheckout(checkout, null);

        assertEquals(new RetornoCheckout(reserva.getCodigo(), null), otraVez);
        assertEquals(1, pagoRepository.findByReservaIdIn(List.of(reserva.getId())).size());
    }

    // R30: candy pendiente de la reserva, 2 pochoclos de 4000.
    private Reserva reservarConCandy() {
        int pochoclos = carta.agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(4000), ImagenUrl.NINGUNA).getId();
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        candy.pedirParaReserva(reserva.getId(), Map.of(pochoclos, 2));
        return reserva;
    }

    private CompraCandy candyDe(Reserva reserva) {
        return candy.comprasDeReservas(List.of(reserva.getId())).get(0);
    }

    private void cincuentaOff() {
        promociones.crear(TipoPromocion.PORCENTAJE, "50 off", ParametrosPromocion.dePorcentaje(50.0),
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()), ImagenUrl.NINGUNA);
    }

    // R30: un solo cobro, con el mismo medio y la misma autorización; el pago guarda solo las entradas.
    @Test
    void cobrarEnCajaCobraTambienElCandyPendiente() {
        Reserva reserva = reservarConCandy();

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.DEBITO, "AUT-5");

        assertEquals(Dinero.de(5000), pago.getMonto(), "el borderó y la DJ siguen leyendo solo entradas");
        CompraCandy cobrada = candyDe(reserva);
        assertEquals(EstadoCompraCandy.COBRADA, cobrada.getEstado());
        assertEquals(MedioPago.DEBITO, cobrada.getMedio());
        assertEquals("AUT-5", cobrada.getCodigoAutorizacion());
        assertEquals(pago.getFecha(), cobrada.getCobradaEn());
        assertEquals(Dinero.de(8000), caja.arqueoCandyDe(reloj.hoy()).total());
    }

    // D4: las promos son solo de entradas (R15, R16); el candy ya trae el ahorro de sus combos (R14).
    @Test
    void laCotizacionDescuentaSoloLasEntradasYSumaElCandy() {
        cincuentaOff();
        Reserva reserva = reservarConCandy();

        List<Cotizacion> cotizaciones = pagos.cotizar(reserva.getId());

        assertEquals(List.of(MedioPago.DEBITO, MedioPago.CREDITO, MedioPago.QR, MedioPago.TRANSFERENCIA),
                cotizaciones.stream().map(Cotizacion::medio).toList(), "solo los medios con checkout");
        Cotizacion qr = cotizaciones.get(2);
        assertEquals(Dinero.de(5000), qr.entradas());
        assertEquals(Dinero.de(2500), qr.descuento().monto());
        assertEquals("50 off", qr.descuento().promocion());
        assertEquals(Dinero.de(8000), qr.candy());
        assertEquals(Dinero.de(10500), qr.total());
    }

    // La cotización no puede prometer otro monto que el que pide el checkout.
    @Test
    void elCheckoutPideLoMismoQueLaCotizacionYAlConfirmarCobraElCandy() {
        cincuentaOff();
        Reserva reserva = reservarConCandy();
        Dinero cotizado = pagos.cotizar(reserva.getId()).get(2).total();

        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR);
        Pago pago = pagos.confirmarCheckout(checkout.id());

        assertEquals(cotizado, checkout.monto());
        assertEquals(Dinero.de(2500), pago.getMonto());
        assertEquals(pago.getCodigoAutorizacion(), candyDe(reserva).getCodigoAutorizacion());
        assertEquals(EstadoCompraCandy.COBRADA, candyDe(reserva).getEstado());
    }

    // Lo pagado en la pasarela es el monto de cuando se abrió: si el candy cambió, no se autoriza (no hay devolución, R13).
    @Test
    void siElCandyCambiaDespuesDeAbrirElCheckoutNoSeCobra() {
        PasarelaQueCuenta pasarela = new PasarelaQueCuenta();
        GestorPagos conEsaPasarela = new GestorPagos(pagoRepository, reservaRepository, politica,
                pasarela, candy, eventos, reloj);
        Reserva reserva = reservarConCandy();
        String checkout = new TransactionTemplate(transacciones).execute(
                estado -> conEsaPasarela.iniciarCheckout(reserva.getId(), MedioPago.QR)).id();
        candy.pedirParaReserva(reserva.getId(), Map.of());

        rechaza("El monto cambió después de abrir el pago: acercate a la boletería",
                () -> new TransactionTemplate(transacciones)
                        .executeWithoutResult(estado -> conEsaPasarela.confirmarCheckout(checkout)));

        assertEquals(0, pasarela.autorizaciones);
        assertFalse(pagoRepository.existsByReservaId(reserva.getId()));
    }

    @Test
    void noSeCotizaUnaReservaQueYaNoSePuedeCobrar() {
        Reserva reserva = reservarConCandy();
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        rechaza("La reserva está pagada: no se puede cobrar", () -> pagos.cotizar(reserva.getId()));
    }

    private static class PasarelaQueNoAprueba extends MercadoPagoEmulado {

        @Override
        public String autorizar(Checkout checkout) {
            throw new ConflictoDeNegocio("Mercado Pago rechazó el pago");
        }
    }

    private static class PasarelaQueCuenta extends MercadoPagoEmulado {

        private int autorizaciones;
        private LocalDateTime vence;

        @Override
        public Checkout crear(int reservaId, MedioPago medio, Dinero entradas, Dinero candy, LocalDateTime vence) {
            this.vence = vence;
            return super.crear(reservaId, medio, entradas, candy, vence);
        }

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
