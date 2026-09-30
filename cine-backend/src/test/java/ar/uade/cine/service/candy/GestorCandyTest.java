package ar.uade.cine.service.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
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
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;

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

    private int pochoclos;
    private int gaseosa;

    @BeforeEach
    void prepararCarta() {
        clientes.registrar("Andrei", "andrei@uade.edu.ar");
        pochoclos = carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000)).getId();
        gaseosa = carta.agregar("Gaseosa 500ml", TipoProducto.BEBIDA, Dinero.de(2500)).getId();
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
        assertThrows(Rechazo.class,
                () -> carta.armarCombo("Combo caro", Dinero.de(6500), pochoclosYGaseosa()));
        assertThrows(Rechazo.class,
                () -> carta.armarCombo("Combo carísimo", Dinero.de(7000), pochoclosYGaseosa()));
    }

    @Test
    void armaElComboYGuardaQueTrae() {
        Producto combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa());

        assertTrue(combo.esCombo());
        assertEquals(2, combo.getComponentes().size());
        assertEquals(TipoProducto.COMBO, combo.getTipo());
    }

    @Test
    void unComboNecesitaAlMenosDosProductos() {
        assertThrows(Rechazo.class,
                () -> carta.armarCombo("Combo de uno", Dinero.de(3000), pedido(pochoclos, 1)));
    }

    @Test
    void unComboNoPuedeContenerOtroCombo() {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa()).getId();

        Map<Integer, Integer> anidado = pedido(combo, 1);
        anidado.put(gaseosa, 1);
        assertThrows(Rechazo.class,
                () -> carta.armarCombo("Combo del combo", Dinero.de(6000), anidado));
    }

    @Test
    void elAhorroEsLaDiferenciaContraComprarloSuelto() {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa()).getId();

        CompraCandy compra = candy.vender(1, pedido(combo, 2), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(11000.0), compra.getTotal());
        assertEquals(Dinero.de(2000.0), compra.getAhorro(), "6500 sueltos contra 5500, por dos combos");
    }

    @Test
    void editarElComboDespuesDeVenderNoCambiaElAhorroDeLaCompra() {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa()).getId();
        candy.vender(1, pedido(combo, 2), MedioPago.EFECTIVO, "");

        carta.editar(combo, "Combo pareja", Dinero.de(6000));

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

        assertThrows(Rechazo.class,
                () -> candy.vender(1, pedido(gaseosa, 1), MedioPago.EFECTIVO, ""));
        assertEquals(1, carta.listarDisponibles().size());
        assertEquals(2, carta.listar().size(), "sacarlo de la carta no lo borra");
    }

    @Test
    void losMediosElectronicosExigenCodigoDeAutorizacion() {
        assertThrows(Rechazo.class,
                () -> candy.vender(1, pedido(pochoclos, 1), MedioPago.CREDITO, "  "));
    }

    @Test
    void rechazaCantidadesInvalidas() {
        assertThrows(Rechazo.class,
                () -> candy.vender(1, pedido(pochoclos, 0), MedioPago.EFECTIVO, ""));
        assertThrows(Rechazo.class,
                () -> candy.vender(1, Map.of(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void emiteElTicketConElDetalleYElAhorro() throws IOException {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa()).getId();
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
        assertThrows(Rechazo.class,
                () -> carta.agregar("pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000)));
        assertThrows(Rechazo.class,
                () -> carta.agregar("Agua", TipoProducto.BEBIDA, Dinero.de(0)));
    }

    // Nombre y precio van antes que el nombre repetido en las tres: el alta ya era así, y la edición y el
    // combo contestaban 409 por el nombre a un pedido que igual no iba a pasar.
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
                case "alta" -> carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, negativo);
                case "edición" -> carta.editar(gaseosa, "Pochoclos grandes", negativo);
                default -> carta.armarCombo("Pochoclos grandes", negativo, pochoclosYGaseosa());
            }
        });

        assertEquals("El precio tiene que ser mayor a cero", error.getMessage());
    }

    @Test
    void unComboNoSeDaDeAltaComoProductoSuelto() {
        assertThrows(Rechazo.class,
                () -> carta.agregar("Combo trucho", TipoProducto.COMBO, Dinero.de(5000)));
    }

    @Test
    void seVendeSinClienteIdentificado() {
        Producto pochoclos = carta.agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));

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
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
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

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> candy.venderParaReserva(reserva.getId(),
                null, pedido(pochoclos, 1), MedioPago.EFECTIVO, ""));

        assertEquals("La reserva " + reserva.getId() + " no está pagada: cobrala antes de agregarle candy",
                error.getMessage());
        assertEquals(List.of(), candy.listarComprasDe(1));
    }

    @Test
    void siElPedidoNombraAlClienteTieneQueSerElDeLaReserva() {
        Reserva reserva = reservarYCobrar();
        int otro = clientes.registrar("Otra", "otra@uade.edu.ar").getId();

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> candy.venderParaReserva(reserva.getId(),
                otro, pedido(pochoclos, 1), MedioPago.EFECTIVO, ""));

        assertEquals("La reserva " + reserva.getId() + " es de otro cliente: revisá la reserva o el cliente",
                error.getMessage());
        assertEquals(reserva.getId(), candy.venderParaReserva(reserva.getId(), 1, pedido(pochoclos, 1),
                MedioPago.EFECTIVO, "").getReservaId(), "con el mismo cliente, se vende");
    }

    // Quién es igual a quién lo decide la base y no Java: la collation de MySQL ignora acentos ("Maní" y
    // "Mani") y el UPPER de H2 convierte la ß en SS. Comparando en Java, el producto chocaba consigo mismo.
    @Test
    void renombrarUnProductoComoLoComparaLaBaseNoChocaConsigoMismo() {
        int weiss = carta.agregar("Weiß", TipoProducto.BEBIDA, Dinero.de(3000)).getId();

        assertEquals("WEISS", carta.editar(weiss, "WEISS", Dinero.de(3000)).getNombre());
    }

    @Test
    void renombrarloComoOtroProductoEsRepetido() {
        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                () -> carta.editar(gaseosa, " pochoclos GRANDES ", Dinero.de(2500)));

        assertEquals("Ya existe un producto con ese nombre", error.getMessage());
    }
}
