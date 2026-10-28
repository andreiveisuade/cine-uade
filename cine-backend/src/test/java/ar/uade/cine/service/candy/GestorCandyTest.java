package ar.uade.cine.service.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.model.imagenes.ImagenUrl;
import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.EstadoCompraCandy;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.informes.ArqueoCandy;
import ar.uade.cine.service.informes.GestorCaja;
import ar.uade.cine.service.informes.GestorInformes;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.Ocupacion;

class GestorCandyTest extends PruebaDeIntegracion {

    private static final Path DIRECTORIO_TICKETS = Path.of("target/comprobantes/tickets");

    @Autowired
    private GestorCandy candy;
    @Autowired
    private GestorCaja caja;
    @Autowired
    private GestorProductos carta;
    @Autowired
    private GestorClientes clientes;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorPagos pagos;
    @Autowired
    private GestorInformes informes;
    @Autowired
    private Ocupacion ocupacion;

    private int pochoclos;
    private int gaseosa;

    @BeforeEach
    void prepararCarta() {
        clientes.ingresarConGoogle("Andrei", "andrei@uade.edu.ar");
        pochoclos = carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000), ImagenUrl.NINGUNA).getId();
        gaseosa = carta.agregar("Gaseosa 500ml", TipoProducto.BEBIDA, Dinero.de(2500), ImagenUrl.NINGUNA).getId();
    }

    private Map<Integer, Integer> pedido(int idA, int cantidadA) {
        Map<Integer, Integer> items = new LinkedHashMap<>();
        items.put(idA, cantidadA);
        return items;
    }

    private Map<Integer, Integer> pochoclosYGaseosa() {
        Map<Integer, Integer> items = pedido(pochoclos, 1);
        items.put(gaseosa, 1);
        return items;
    }

    @Test
    void elComboTieneQueSalirMenosQueSusComponentes() {
        rechaza("El combo tiene que salir menos que sus componentes sueltos ($ 6500.00)",
                () -> carta.armarCombo("Combo caro", Dinero.de(6500), pochoclosYGaseosa(), ImagenUrl.NINGUNA));
        rechaza("El combo tiene que salir menos que sus componentes sueltos ($ 6500.00)",
                () -> carta.armarCombo("Combo carísimo", Dinero.de(7000), pochoclosYGaseosa(), ImagenUrl.NINGUNA));
    }

    @Test
    @DisplayName("R14: abaratar un suelto no puede dejar sin convenir al combo que lo trae")
    void abaratarUnSueltoNoPuedeDejarSinConvenirAlCombo() {
        carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa(), ImagenUrl.NINGUNA);

        Rechazo error = rechaza("Con ese precio, el combo Combo pareja dejaría de salir menos que sus"
                        + " componentes sueltos ($ 4500.00)",
                () -> carta.editar(pochoclos, "Pochoclos grandes", Dinero.de(2000), ImagenUrl.NINGUNA));

        assertInstanceOf(DatoInvalido.class, error);
        assertEquals(Dinero.de(4000), carta.obtener(pochoclos).getPrecio());
    }

    @Test
    void armaElComboYGuardaQueTrae() {
        Producto combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa(), ImagenUrl.NINGUNA);

        assertTrue(combo.esCombo());
        assertEquals(2, combo.getComponentes().size());
        assertEquals(TipoProducto.COMBO, combo.getTipo());
    }

    @Test
    void unComboNecesitaAlMenosDosProductos() {
        rechaza("Un combo tiene que juntar al menos dos productos distintos",
                () -> carta.armarCombo("Combo de uno", Dinero.de(3000), pedido(pochoclos, 1), ImagenUrl.NINGUNA));
    }

    @Test
    void unComboNoPuedeContenerOtroCombo() {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa(), ImagenUrl.NINGUNA).getId();

        Map<Integer, Integer> anidado = pedido(combo, 1);
        anidado.put(gaseosa, 1);
        rechaza("Un combo no puede contener otro combo: Combo pareja",
                () -> carta.armarCombo("Combo del combo", Dinero.de(6000), anidado, ImagenUrl.NINGUNA));
    }

    @Test
    void elAhorroEsLaDiferenciaContraComprarloSuelto() {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa(), ImagenUrl.NINGUNA).getId();

        CompraCandy compra = candy.vender(1, pedido(combo, 2), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(11000.0), compra.getTotal());
        assertEquals(Dinero.de(2000.0), compra.getAhorro(), "6500 sueltos contra 5500, por dos combos");
    }

    @Test
    void editarElComboDespuesDeVenderNoCambiaElAhorroDeLaCompra() {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa(), ImagenUrl.NINGUNA).getId();
        candy.vender(1, pedido(combo, 2), MedioPago.EFECTIVO, "");

        carta.editar(combo, "Combo pareja", Dinero.de(6000), ImagenUrl.NINGUNA);

        CompraCandy guardada = candy.listarComprasDe(1).get(0);
        assertEquals(Dinero.de(2000.0), guardada.getAhorro(), "el ahorro de cuando se vendió, no el de hoy");
    }

    @Test
    void elTotalSaleDeLaCartaYNoDeQuienVende() {
        CompraCandy compra = candy.vender(1, pochoclosYGaseosa(), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(6500.0), compra.getTotal());
        assertEquals(2, compra.getItems().size());
    }

    @Test
    void noSeVendeUnProductoQueSeSacoDeLaCarta() {
        carta.sacarDeLaVenta(gaseosa);

        rechaza("Gaseosa 500ml no está disponible",
                () -> candy.vender(1, pedido(gaseosa, 1), MedioPago.EFECTIVO, ""));
        assertEquals(1, carta.listarDisponibles().size());
        assertEquals(2, carta.listar().size(), "sacarlo de la carta no lo borra");
    }

    @Test
    void losMediosElectronicosExigenCodigoDeAutorizacion() {
        rechaza("Falta el código de autorización del pago con crédito",
                () -> candy.vender(1, pedido(pochoclos, 1), MedioPago.CREDITO, "  "));
    }

    @Test
    void rechazaCantidadesInvalidas() {
        rechaza("La cantidad de Pochoclos grandes tiene que ser mayor a cero",
                () -> candy.vender(1, pedido(pochoclos, 0), MedioPago.EFECTIVO, ""));
        rechaza("Hay que elegir al menos un producto",
                () -> candy.vender(1, Map.of(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void emiteElTicketConElDetalleYElAhorro() throws IOException {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa(), ImagenUrl.NINGUNA).getId();
        CompraCandy compra = candy.vender(1, pedido(combo, 1), MedioPago.DEBITO, "AUT-77");

        Path ticket = DIRECTORIO_TICKETS.resolve("candy-" + compra.getId() + ".txt");
        assertTrue(Files.exists(ticket), "no se generó el ticket del candy");

        String contenido = Files.readString(ticket);
        assertTrue(contenido.contains("Andrei"), "el ticket no menciona al cliente");
        assertTrue(contenido.contains("Combo pareja"), "el ticket no lista el combo");
        assertTrue(contenido.contains("5500"), "el ticket no muestra el total");
        assertTrue(contenido.contains("Ahorraste"), "el ticket no muestra el ahorro del combo");
        assertTrue(contenido.contains("AUT-77"), "el ticket no muestra la autorización");
    }

    @Test
    void elArqueoDelCandySumaLoVendidoEnElDia() {
        candy.vender(1, pedido(pochoclos, 2), MedioPago.EFECTIVO, "");
        candy.vender(1, pedido(gaseosa, 1), MedioPago.QR, "QR-1");

        ArqueoCandy arqueo = caja.arqueoCandyDe(reloj.hoy());

        assertEquals(2, arqueo.compras().size());
        assertEquals(Dinero.de(10500.0), arqueo.total());
    }

    @Test
    void rechazaProductoRepetidoOPrecioInvalido() {
        rechaza("Ya existe un producto con ese nombre",
                () -> carta.agregar("pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000), ImagenUrl.NINGUNA));
        rechaza("El precio tiene que ser mayor a cero",
                () -> carta.agregar("Agua", TipoProducto.BEBIDA, Dinero.de(0), ImagenUrl.NINGUNA));
    }

    // Nombre y precio van antes que el nombre repetido en las tres: el 409 no puede tapar un pedido inválido.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            alta con precio negativo,    alta
            edición con precio negativo, edición
            combo con precio negativo,   combo
            """)
    void unPrecioInvalidoSeRechazaAntesQueElNombreRepetido(String caso, String operacion) {
        Dinero negativo = Dinero.de(-5);
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> {
            switch (operacion) {
                case "alta" -> carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, negativo, ImagenUrl.NINGUNA);
                case "edición" -> carta.editar(gaseosa, "Pochoclos grandes", negativo, ImagenUrl.NINGUNA);
                default -> carta.armarCombo("Pochoclos grandes", negativo, pochoclosYGaseosa(), ImagenUrl.NINGUNA);
            }
        });

        assertEquals("El precio tiene que ser mayor a cero", error.getMessage());
    }

    @Test
    void unComboNoSeDaDeAltaComoProductoSuelto() {
        rechaza("Un combo se da de alta como combo: con sus componentes",
                () -> carta.agregar("Combo trucho", TipoProducto.COMBO, Dinero.de(5000), ImagenUrl.NINGUNA));
    }

    @Test
    void seVendeSinClienteIdentificado() {
        Producto pochoclos = carta.agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000), ImagenUrl.NINGUNA);

        CompraCandy compra = candy.vender(null, Map.of(pochoclos.getId(), 1),
                MedioPago.EFECTIVO, "");

        assertNull(compra.getClienteId());
        assertNull(compra.getReservaId());
        assertEquals(Dinero.de(3000), compra.getTotal());
    }

    // Una reserva de mañana del cliente 1, sin cobrar.
    private Reserva reservar() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5));
        funciones.programar(1, 1, reloj.ahora().plusDays(1),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        return reservas.reservar(1, 1, Map.of("A1", TipoTarifa.GENERAL), null);
    }

    private Reserva reservarYCobrar() {
        Reserva reserva = reservar();
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        return reserva;
    }

    @Test
    void laCompraDesdeUnaReservaHeredaSuCliente() {
        Reserva reserva = reservarYCobrar();

        CompraCandy compra = candy.venderParaReserva(reserva.getId(), null,
                pedido(pochoclos, 2), MedioPago.EFECTIVO, "");

        assertEquals(reserva.getId(), compra.getReservaId());
        assertEquals(reserva.getClienteId(), compra.getClienteId(), "el cliente sale de la reserva");
    }

    @Test
    void noSeAgregaCandyAUnaReservaInexistente() {
        RecursoNoEncontrado error = assertThrows(RecursoNoEncontrado.class, () -> candy.venderParaReserva(999,
                null, pedido(pochoclos, 1), MedioPago.EFECTIVO, ""));

        assertEquals("No existe la reserva 999", error.getMessage());
    }

    // El candy se retira con el QR de la entrada: una reserva sin cobrar o cancelada no tiene entrada.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin cobrar, false
            cancelada,  true
            """)
    void soloUnaReservaPagadaLlevaCandy(String caso, boolean cancelarla) {
        Reserva reserva = reservar();
        if (cancelarla) {
            reservas.cancelar(reserva.getId());
        }

        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class, () -> candy.venderParaReserva(reserva.getId(),
                null, pedido(pochoclos, 1), MedioPago.EFECTIVO, ""));

        assertEquals("La reserva " + reserva.getId() + " no está pagada: cobrala antes de agregarle candy",
                error.getMessage());
        assertEquals(List.of(), candy.listarComprasDe(1));
    }

    @Test
    void siElPedidoNombraAlClienteTieneQueSerElDeLaReserva() {
        Reserva reserva = reservarYCobrar();
        int otro = clientes.ingresarConGoogle("Otra", "otra@uade.edu.ar").getId();

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> candy.venderParaReserva(reserva.getId(),
                otro, pedido(pochoclos, 1), MedioPago.EFECTIVO, ""));

        assertEquals("La reserva " + reserva.getId() + " es de otro cliente: revisá la reserva o el cliente",
                error.getMessage());
        assertEquals(reserva.getId(), candy.venderParaReserva(reserva.getId(), 1, pedido(pochoclos, 1),
                MedioPago.EFECTIVO, "").getReservaId(), "con el mismo cliente, se vende");
    }

    // La igualdad la decide la base: la collation de MySQL ignora acentos y el UPPER de H2 convierte ß en SS.
    @Test
    void renombrarUnProductoComoLoComparaLaBaseNoChocaConsigoMismo() {
        int weiss = carta.agregar("Weiß", TipoProducto.BEBIDA, Dinero.de(3000), ImagenUrl.NINGUNA).getId();

        assertEquals("WEISS", carta.editar(weiss, "WEISS", Dinero.de(3000), ImagenUrl.NINGUNA).getNombre());
    }

    @Test
    void renombrarloComoOtroProductoEsRepetido() {
        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                () -> carta.editar(gaseosa, " pochoclos GRANDES ", Dinero.de(2500), ImagenUrl.NINGUNA));

        assertEquals("Ya existe un producto con ese nombre", error.getMessage());
    }

    private EstadoCompraCandy estadoDelPedido(int reservaId) {
        return candy.listarComprasDe(1).stream()
                .filter(compra -> compra.getReservaId() == reservaId)
                .findFirst().orElseThrow().getEstado();
    }

    @Test
    @DisplayName("R30: el candy pedido en la web queda pendiente, con los precios de la carta de ese momento")
    void elPedidoDeLaWebQuedaPendienteConLosPreciosCongelados() {
        Reserva reserva = reservar();

        candy.pedirParaReserva(reserva.getId(), pedido(pochoclos, 2));
        carta.editar(pochoclos, "Pochoclos grandes", Dinero.de(4500), ImagenUrl.NINGUNA);

        CompraCandy pedido = candy.pendienteDe(reserva.getId()).orElseThrow();
        assertEquals(EstadoCompraCandy.PENDIENTE, pedido.getEstado());
        assertEquals(reserva.getClienteId(), pedido.getClienteId());
        assertNull(pedido.getMedio());
        assertEquals(Dinero.de(8000), pedido.getTotal());
    }

    @Test
    void pedirDeNuevoReemplazaElPedidoYVacioLoSaca() {
        Reserva reserva = reservar();
        candy.pedirParaReserva(reserva.getId(), pedido(pochoclos, 2));

        candy.pedirParaReserva(reserva.getId(), pochoclosYGaseosa());
        assertEquals(1, candy.listarComprasDe(1).size());
        assertEquals(Dinero.de(6500), candy.pendienteDe(reserva.getId()).orElseThrow().getTotal());

        candy.pedirParaReserva(reserva.getId(), Map.of());
        assertEquals(List.of(), candy.listarComprasDe(1));
    }

    // Los topes del mostrador; un pedido rechazado deja el anterior como estaba.
    @Test
    void elPedidoTieneLosTopesDelMostrador() {
        Reserva reserva = reservar();
        candy.pedirParaReserva(reserva.getId(), pedido(pochoclos, 1));
        carta.sacarDeLaVenta(gaseosa);

        rechaza("La cantidad de Pochoclos grandes tiene que ser como máximo 20",
                () -> candy.pedirParaReserva(reserva.getId(), pedido(pochoclos, 21)));
        rechaza("Gaseosa 500ml no está disponible",
                () -> candy.pedirParaReserva(reserva.getId(), pedido(gaseosa, 1)));
        rechaza("No existe el producto 99", () -> candy.pedirParaReserva(reserva.getId(), pedido(99, 1)));
        assertEquals(Dinero.de(4000), candy.pendienteDe(reserva.getId()).orElseThrow().getTotal());
    }

    // R30: se cobra con la reserva, así que solo mientras se pueda cobrar (R5, R17).
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            pagada    | La reserva está pagada: no se puede cobrar
            cancelada | La reserva está cancelada: no se puede cobrar
            vencida   | venció: sus butacas volvieron a estar disponibles
            """)
    void soloSePideCandyParaUnaReservaQueSePuedeCobrar(String caso, String mensaje) {
        Reserva reserva = reservar();
        switch (caso) {
            case "pagada" -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
            case "cancelada" -> reservas.cancelar(reserva.getId());
            default -> reloj.mover(reloj.ahora().plusMinutes(Reserva.MINUTOS_PARA_PAGAR + 1));
        }

        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                () -> candy.pedirParaReserva(reserva.getId(), pedido(pochoclos, 1)));

        assertTrue(error.getMessage().contains(mensaje), error.getMessage());
        assertEquals(List.of(), candy.listarComprasDe(1));
    }

    // R30: si la reserva se cancela o vence, el pedido no se va a cobrar.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            cancelar la reserva, true
            que venza,           false
            """)
    void cancelarOVencerLaReservaAnulaElPedido(String caso, boolean cancelarla) {
        Reserva reserva = reservar();
        candy.pedirParaReserva(reserva.getId(), pedido(pochoclos, 1));

        if (cancelarla) {
            reservas.cancelar(reserva.getId());
        } else {
            reloj.mover(reloj.ahora().plusMinutes(Reserva.MINUTOS_PARA_PAGAR + 1));
            ocupacion.asientosOcupados(reserva.getFuncionId(), null);
        }

        assertEquals(EstadoCompraCandy.ANULADA, estadoDelPedido(reserva.getId()), caso);
        assertTrue(candy.pendienteDe(reserva.getId()).isEmpty());
        assertEquals(List.of(), candy.comprasDeReservas(List.of(reserva.getId())), "lo anulado no figura en la reserva");
    }

    // El arqueo cuenta por cobro: el pedido sin cobrar no entró a la caja y el cobrado entra el día del pago.
    @Test
    void elArqueoCuentaElPedidoRecienCuandoSeCobra() {
        Reserva reserva = reservar();
        candy.pedirParaReserva(reserva.getId(), pedido(pochoclos, 1));
        assertEquals(0, caja.arqueoCandyDe(reloj.hoy()).compras().size());
        assertEquals(0, informes.informeDe(reserva.getFuncionId()).comprasCandy());

        reloj.mover(reloj.ahora().plusMinutes(10));
        pagos.cobrar(reserva.getId(), MedioPago.DEBITO, "AUT-9");

        ArqueoCandy arqueo = caja.arqueoCandyDe(reloj.hoy());
        assertEquals(1, arqueo.compras().size());
        assertEquals(Dinero.de(4000), arqueo.total());
        assertEquals(reloj.ahora(), arqueo.compras().get(0).getCobradaEn());
        assertEquals(Dinero.de(4000), informes.informeDe(reserva.getFuncionId()).candy());
    }

    // Lo anulado tampoco es venta del día ni de la función.
    @Test
    void elPedidoAnuladoNoEntraAlArqueoNiAlInforme() {
        Reserva reserva = reservar();
        candy.pedirParaReserva(reserva.getId(), pedido(pochoclos, 1));
        reservas.cancelar(reserva.getId());

        assertEquals(Dinero.CERO, caja.arqueoCandyDe(reloj.hoy()).total());
        assertEquals(0, informes.informeDe(reserva.getFuncionId()).comprasCandy());
    }

    @Test
    void elMostradorConUnaReservaSinPagarSigueRechazandoAunqueTengaPedidoDeLaWeb() {
        Reserva reserva = reservar();
        candy.pedirParaReserva(reserva.getId(), pedido(pochoclos, 1));

        rechaza("La reserva " + reserva.getId() + " no está pagada: cobrala antes de agregarle candy",
                () -> candy.venderParaReserva(reserva.getId(), null, pedido(gaseosa, 1), MedioPago.EFECTIVO, ""));
    }
}
