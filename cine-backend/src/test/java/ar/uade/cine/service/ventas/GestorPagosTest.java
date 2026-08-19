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
import org.junit.jupiter.api.io.TempDir;

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
import ar.uade.cine.repository.AsientoDAO;
import ar.uade.cine.repository.ClienteDAO;
import ar.uade.cine.repository.FuncionDAO;
import ar.uade.cine.repository.PagoDAO;
import ar.uade.cine.repository.PeliculaDAO;
import ar.uade.cine.repository.ReservaDAO;
import ar.uade.cine.repository.SalaDAO;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.repository.memoria.AsientoDAOMemoria;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.repository.memoria.ClienteDAOMemoria;
import ar.uade.cine.repository.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.repository.memoria.FuncionDAOMemoria;
import ar.uade.cine.repository.memoria.PagoDAOMemoria;
import ar.uade.cine.repository.memoria.PeliculaDAOMemoria;
import ar.uade.cine.repository.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.repository.memoria.PromocionDAOMemoria;
import ar.uade.cine.repository.memoria.ReservaDAOMemoria;
import ar.uade.cine.repository.memoria.SalaDAOMemoria;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.informes.Arqueo;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.promociones.GestorPromociones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.informes.GestorCaja;
import ar.uade.cine.model.dinero.Dinero;

/** R5: solo se cobra una reserva en estado RESERVADA, y una sola vez. */
class GestorPagosTest {

    @TempDir
    Path tempDir;

    private GestorReservas reservas;
    private GestorPagos pagos;
    private GestorCaja caja;
    private GestorPromociones promociones;
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
        reservaDAO = new ReservaDAOMemoria();

        GestorFunciones gestorFunciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        new GestorCartelera(peliculaDAO, funcionDAO, new GestorProgramaciones(
                new ProgramacionDAOMemoria(), funcionDAO, gestorFunciones))
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        new GestorSalas(salaDAO, asientoDAO, funcionDAO).agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        gestorFunciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
        new GestorClientes(clienteDAO, reservaDAO, new CompraCandyDAOMemoria()).registrar("Andrei", "andrei@uade.edu.ar");

        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO, peliculaDAO,
                new GeneradorTicketTxt(tempDir.resolve("tickets")), new CalculadoraPrecio(),
                new Ocupacion(reservaDAO, funcionDAO, asientoDAO, new BloqueoButacasMemoria()));
        promociones = new GestorPromociones(new PromocionDAOMemoria());
        pagos = new GestorPagos(pagoDAO, reservaDAO, funcionDAO, promociones,
                new MercadoPagoEmulado(), new GeneradorReciboTxt(tempDir.resolve("tickets")));
        // El cierre de caja es otro gestor: cobrar y arquear cambian por motivos distintos.
        caja = new GestorCaja(pagoDAO, reservaDAO, new CompraCandyDAOMemoria());
    }

    @Test
    void elMontoSaleDeLaReservaYNoDeQuienCobra() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2"));

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(10000.0), pago.getMonto());
        assertEquals(reserva.getTotal(), pago.getMonto());
    }

    @Test
    void cobrarDejaLaReservaPagada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        pagos.cobrar(reserva.getId(), MedioPago.DEBITO, "AUT-123");

        assertEquals(EstadoReserva.PAGADA,
                reservaDAO.buscarPorId(reserva.getId()).orElseThrow().getEstado());
    }

    @Test
    void noSeCobraDosVecesLaMismaReserva() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void noSeCobraUnaReservaCancelada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        reservas.cancelar(reserva.getId());

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void losMediosElectronicosExigenCodigoDeAutorizacion() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.CREDITO, "  "));
    }

    @Test
    void elEfectivoNoNecesitaCodigo() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals("", pago.getCodigoAutorizacion());
    }

    @Test
    void elArqueoDeBoleteriaSumaLoCobradoEnElDia() {
        Reserva primera = reservas.reservar(1, 1, generales("A1", "A2"));
        Reserva segunda = reservas.reservar(1, 1, generales("B1"));
        pagos.cobrar(primera.getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(segunda.getId(), MedioPago.QR, "QR-99");

        assertEquals(2, caja.listarDelDia(LocalDate.now()).size());
        assertEquals(Dinero.de(15000.0), caja.totalCobrado(LocalDate.now()));
        assertTrue(pagos.buscarPorReserva(primera.getId()).isPresent());
    }


    // ---------- el cobro es donde se resuelve el descuento ----------

    /**
     * El total definitivo no existe hasta que se cobra: recién ahí se sabe el medio de
     * pago, y con él qué promociones corren.
     */
    @Test
    void elPagoGuardaSubtotalDescuentoYPromocion() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2"));
        Promocion promo = promociones.crearNxM("2x1", 2, 1,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertEquals(reserva.getTotal(), pago.getSubtotal());
        assertEquals(promo.getId(), pago.getPromocionId());
        assertEquals(pago.getSubtotal().menos(pago.getDescuento()), pago.getMonto());
        assertTrue(pago.getDescuento().esMayorQue(Dinero.CERO));
    }

    @Test
    void sinPromocionAplicableElMontoEsElSubtotal() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));

        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertNull(pago.getPromocionId());
        assertEquals(Dinero.de(0), pago.getDescuento());
        assertEquals(pago.getSubtotal(), pago.getMonto());
    }

    /** El descuento del banco depende del medio, que se elige acá y no al reservar. */
    @Test
    void elDescuentoBancarioSoloEntraSiSePagaConEseMedio() {
        promociones.crearMontoFijo("Banco", Dinero.de(1000),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of(MedioPago.CREDITO));

        Reserva enEfectivo = reservas.reservar(1, 1, generales("A1"));
        Reserva conTarjeta = reservas.reservar(1, 1, generales("A2"));

        assertEquals(Dinero.de(0), pagos.cobrar(enEfectivo.getId(), MedioPago.EFECTIVO, "").getDescuento());
        assertEquals(Dinero.de(1000), pagos.cobrar(conTarjeta.getId(), MedioPago.CREDITO, "AUT-1").getDescuento());
    }

    /** El arqueo suma lo que entró en la caja, no lo que salía de lista. */
    @Test
    void elArqueoCuentaElMontoCobradoYNoElSubtotal() {
        promociones.crearPorcentaje("50 off", 50,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Dinero arqueo = Dinero.sumar(caja.listarDelDia(LocalDate.now()).stream()
                .map(Pago::getMonto).toList());

        assertEquals(pago.getMonto(), arqueo);
        assertTrue(pago.getSubtotal().esMayorQue(arqueo));
    }

    // ---------- el cierre de caja ----------

    /**
     * El arqueo es una cuenta del gestor y no de quien lo muestra: la consola y la API
     * tienen que dar estos mismos tres números.
     */
    @Test
    void elArqueoResumeTotalEntradasYRepartoPorMedio() {
        Reserva primera = reservas.reservar(1, 1, generales("A1", "A2"));
        Reserva segunda = reservas.reservar(1, 1, generales("B1"));
        pagos.cobrar(primera.getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(segunda.getId(), MedioPago.QR, "QR-99");

        Arqueo arqueo = caja.arqueoDe(LocalDate.now());

        assertEquals(Dinero.de(15000.0), arqueo.total());
        // Tres butacas vendidas en dos cobros: el número no sale de la cantidad de pagos.
        assertEquals(3, arqueo.entradas());
        assertEquals(2, arqueo.pagos().size());
        assertEquals(1, arqueo.porMedio().get(MedioPago.EFECTIVO).cantidad());
        assertEquals(Dinero.de(10000.0), arqueo.porMedio().get(MedioPago.EFECTIVO).total());
        assertEquals(Dinero.de(5000.0), arqueo.porMedio().get(MedioPago.QR).total());
    }

    /** Un día sin cobros no es un error: es una caja en cero. */
    @Test
    void elArqueoDeUnDiaSinCobrosDaEnCero() {
        Arqueo arqueo = caja.arqueoDe(LocalDate.now().minusDays(1));

        assertEquals(Dinero.de(0), arqueo.total());
        assertEquals(0, arqueo.entradas());
        assertTrue(arqueo.pagos().isEmpty());
        assertTrue(arqueo.porMedio().isEmpty());
    }

    /** Lo que entró por caja es lo cobrado, con el descuento ya aplicado. */
    @Test
    void elRepartoPorMedioCuentaElMontoConDescuento() {
        promociones.crearPorcentaje("50 off", 50,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Arqueo arqueo = caja.arqueoDe(LocalDate.now());

        assertEquals(pago.getMonto(), arqueo.total());
        assertEquals(pago.getMonto(), arqueo.porMedio().get(MedioPago.EFECTIVO).total());
        assertTrue(pago.getSubtotal().esMayorQue(arqueo.total()));
    }

    // ---------- el comprobante del cobro ----------

    /**
     * El efectivo no deja rastro afuera del cine: si no se imprime el recibo, el cliente se
     * va sin constancia de haber pagado.
     */
    @Test
    void elCobroEnEfectivoImprimeElReciboDeCaja() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Path recibo = tempDir.resolve("tickets").resolve("recibo-" + pago.getId() + ".txt");

        assertTrue(Files.exists(recibo));
        assertTrue(leer(recibo).contains("EFECTIVO"));
    }

    /** El electrónico ya tiene su comprobante: el cupón del que salió el código. */
    @Test
    void elCobroElectronicoNoImprimeReciboDeCaja() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.CREDITO, "AUT-123");

        assertFalse(Files.exists(tempDir.resolve("tickets").resolve("recibo-" + pago.getId() + ".txt")));
    }

    /** Lo que el recibo dice y el ticket no puede: el descuento se resuelve recién al cobrar. */
    @Test
    void elReciboMuestraElDescuentoQueSeAplicoAlCobrar() {
        promociones.crearPorcentaje("50 off", 50,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        Pago pago = pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        String recibo = leer(tempDir.resolve("tickets").resolve("recibo-" + pago.getId() + ".txt"));

        assertTrue(recibo.contains("Descuento"));
        assertTrue(recibo.contains("2500.00"));
    }

    // ---------- el pago electrónico, contra la pasarela emulada ----------

    @Test
    void elCheckoutViajaConElLinkYElQrDeLaPasarela() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));

        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR);

        assertEquals(reserva.getId(), checkout.reservaId());
        assertEquals(MedioPago.QR, checkout.medio());
        assertEquals(Dinero.de(5000.0), checkout.monto());
        assertFalse(checkout.urlPago().isBlank());
        assertFalse(checkout.codigoQr().isBlank());
    }

    /** El cliente aprueba un importe en la pantalla del procesador: tiene que ser el final. */
    @Test
    void elMontoDelCheckoutYaTraeElDescuentoAplicado() {
        promociones.crearPorcentaje("50 off", 50,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));

        assertEquals(Dinero.de(2500.0), pagos.iniciarCheckout(reserva.getId(), MedioPago.QR).monto());
    }

    /** R11 al revés: el efectivo no tiene a quién pedirle una autorización. */
    @Test
    void elEfectivoNoAbreCheckout() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));

        assertThrows(IllegalArgumentException.class,
                () -> pagos.iniciarCheckout(reserva.getId(), MedioPago.EFECTIVO));
    }

    @Test
    void noSeAbreCheckoutDeUnaReservaYaPagada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertThrows(IllegalArgumentException.class,
                () -> pagos.iniciarCheckout(reserva.getId(), MedioPago.QR));
    }

    /**
     * El código no lo inventa el cine: sale de la pasarela y es lo que después permite
     * reclamarle el cobro. R11 se cumple sin que nadie tipee nada.
     */
    @Test
    void confirmarElCheckoutCobraConElCodigoQueDevolvioLaPasarela() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2"));
        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR);

        Pago pago = pagos.confirmarCheckout(checkout.id());

        assertEquals(reserva.getId(), pago.getReservaId());
        assertEquals(MedioPago.QR, pago.getMedio());
        assertEquals(Dinero.de(10000.0), pago.getMonto());
        assertFalse(pago.getCodigoAutorizacion().isBlank());
        assertEquals(EstadoReserva.PAGADA,
                reservaDAO.buscarPorId(reserva.getId()).orElseThrow().getEstado());
    }

    @Test
    void noSeConfirmaUnCheckoutQueNoExiste() {
        assertThrows(IllegalArgumentException.class, () -> pagos.confirmarCheckout("MP-0000000000"));
    }

    /** Un doble click no cobra dos veces: la segunda confirmación choca contra R5. */
    @Test
    void confirmarDosVecesElMismoCheckoutNoCobraDeNuevo() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(reserva.getId(), MedioPago.QR);
        pagos.confirmarCheckout(checkout.id());

        assertThrows(IllegalArgumentException.class, () -> pagos.confirmarCheckout(checkout.id()));
        assertEquals(1, caja.listarDelDia(LocalDate.now()).size());
    }

    /** Butacas todas con tarifa general, que es el caso base de casi todas las pruebas. */
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
