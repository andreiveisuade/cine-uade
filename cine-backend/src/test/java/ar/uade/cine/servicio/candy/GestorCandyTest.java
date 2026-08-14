package ar.uade.cine.servicio.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.ReservaImpl;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProductoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.servicio.usuarios.GestorClientes;
import ar.uade.cine.servicio.informes.GestorCaja;
import ar.uade.cine.persistencia.memoria.PagoDAOMemoria;
import ar.uade.cine.dominio.dinero.Dinero;

/** R14: un combo tiene que costar menos que sus componentes sueltos. */
class GestorCandyTest {

    @TempDir
    Path tempDir;

    private GestorCandy candy;
    private GestorCaja caja;
    private GestorProductos carta;
    private ReservaDAO reservaDAO;
    private Path directorioTickets;
    private int pochoclos;
    private int gaseosa;

    /** Pochoclos a $4000, gaseosa a $2500, un cliente. */
    @BeforeEach
    void prepararCarta() {
        ClienteDAO clienteDAO = new ClienteDAOMemoria();
        reservaDAO = new ReservaDAOMemoria();
        new GestorClientes(clienteDAO, reservaDAO, new CompraCandyDAOMemoria())
                .registrar("Andrei", "andrei@uade.edu.ar");
        directorioTickets = tempDir.resolve("tickets");

        carta = new GestorProductos(new ProductoDAOMemoria());
        CompraCandyDAOMemoria compraCandyDAO = new CompraCandyDAOMemoria();
        candy = new GestorCandy(compraCandyDAO, clienteDAO, reservaDAO,
                new GeneradorTicketCandyTxt(directorioTickets), carta);
        caja = new GestorCaja(new PagoDAOMemoria(), reservaDAO, compraCandyDAO);
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
        // sueltos son 6500: a 6500 o más no es una promoción
        assertThrows(IllegalArgumentException.class,
                () -> carta.armarCombo("Combo caro", Dinero.de(6500), pochoclosYGaseosa()));
        assertThrows(IllegalArgumentException.class,
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
        assertThrows(IllegalArgumentException.class,
                () -> carta.armarCombo("Combo de uno", Dinero.de(3000), pedido(pochoclos, 1)));
    }

    @Test
    void unComboNoPuedeContenerOtroCombo() {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa()).getId();

        Map<Integer, Integer> anidado = pedido(combo, 1);
        anidado.put(gaseosa, 1);
        assertThrows(IllegalArgumentException.class,
                () -> carta.armarCombo("Combo del combo", Dinero.de(6000), anidado));
    }

    @Test
    void elAhorroEsLaDiferenciaContraComprarloSuelto() {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa()).getId();

        CompraCandy compra = candy.vender(1, pedido(combo, 2), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(11000.0), compra.getTotal());
        assertEquals(Dinero.de(2000.0), carta.ahorroDe(compra), "6500 sueltos contra 5500, por dos combos");
    }

    @Test
    void elTotalSaleDeLaCartaYNoDeQuienVende() {
        CompraCandy compra = candy.vender(1, pochoclosYGaseosa(), MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(6500.0), compra.getTotal());
        assertEquals(2, compra.getItems().size());
    }

    @Test
    void noSeVendeUnProductoQueSeSacoDeLaCarta() {
        carta.cambiarDisponibilidad(gaseosa, false);

        assertThrows(IllegalArgumentException.class,
                () -> candy.vender(1, pedido(gaseosa, 1), MedioPago.EFECTIVO, ""));
        assertEquals(1, carta.listarDisponibles().size());
        assertEquals(2, carta.listar().size(), "sacarlo de la carta no lo borra");
    }

    @Test
    void losMediosElectronicosExigenCodigoDeAutorizacion() {
        assertThrows(IllegalArgumentException.class,
                () -> candy.vender(1, pedido(pochoclos, 1), MedioPago.CREDITO, "  "));
    }

    @Test
    void rechazaCantidadesInvalidas() {
        assertThrows(IllegalArgumentException.class,
                () -> candy.vender(1, pedido(pochoclos, 0), MedioPago.EFECTIVO, ""));
        assertThrows(IllegalArgumentException.class,
                () -> candy.vender(1, Map.of(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void emiteElTicketConElDetalleYElAhorro() throws IOException {
        int combo = carta.armarCombo("Combo pareja", Dinero.de(5500), pochoclosYGaseosa()).getId();
        CompraCandy compra = candy.vender(1, pedido(combo, 1), MedioPago.DEBITO, "AUT-77");

        Path ticket = directorioTickets.resolve("candy-" + compra.getId() + ".txt");
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

        assertEquals(2, candy.listarComprasDelDia(LocalDate.now()).size());
        assertEquals(Dinero.de(10500.0), caja.totalCandyDe(LocalDate.now()));
    }

    @Test
    void rechazaProductoRepetidoOPrecioInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> carta.agregar("pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000)));
        assertThrows(IllegalArgumentException.class,
                () -> carta.agregar("Agua", TipoProducto.BEBIDA, Dinero.de(0)));
    }

    @Test
    void unComboNoSeDaDeAltaComoProductoSuelto() {
        assertThrows(IllegalArgumentException.class,
                () -> carta.agregar("Combo trucho", TipoProducto.COMBO, Dinero.de(5000)));
    }

    /** En el mostrador se compra sin dar el nombre, como en cualquier kiosco. */
    @Test
    void seVendeSinClienteIdentificado() {
        Producto pochoclos = carta.agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));

        CompraCandy compra = candy.vender(null, Map.of(pochoclos.getId(), 1),
                MedioPago.EFECTIVO, "");

        assertNull(compra.getClienteId());
        assertNull(compra.getReservaId());
        assertEquals(Dinero.de(3000), compra.getTotal());
    }

    /** El "¿desea agregar pochoclos?" de después de comprar la entrada por la web. */
    @Test
    void laCompraDesdeUnaReservaHeredaSuCliente() {
        Producto pochoclos = carta.agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));
        Reserva reserva = new ReservaImpl(1, 1,
                List.of(new Entrada(1, "A1", TipoTarifa.GENERAL, Dinero.de(5000))), LocalDateTime.now());
        reservaDAO.guardar(reserva);

        CompraCandy compra = candy.venderParaReserva(reserva.getId(),
                Map.of(pochoclos.getId(), 2), MedioPago.EFECTIVO, "");

        assertEquals(reserva.getId(), compra.getReservaId());
        assertEquals(reserva.getClienteId(), compra.getClienteId(), "el cliente sale de la reserva");
    }

    @Test
    void noSeAgregaCandyAUnaReservaInexistente() {
        Producto pochoclos = carta.agregar("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(3000));

        assertThrows(IllegalArgumentException.class, () -> candy.venderParaReserva(999,
                Map.of(pochoclos.getId(), 1), MedioPago.EFECTIVO, ""));
    }
}
