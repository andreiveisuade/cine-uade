package ar.uade.cine.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.Producto;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.archivo.GeneradorTicketCandyTxt;
import ar.uade.cine.persistencia.archivo.ReservaDAOTxt;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProductoDAOMemoria;

/** R14: un combo tiene que costar menos que sus componentes sueltos. */
class GestorCandyTest {

    @TempDir
    Path tempDir;

    private GestorCandy candy;
    private Path directorioTickets;
    private int pochoclos;
    private int gaseosa;

    /** Pochoclos a $4000, gaseosa a $2500, un cliente. */
    @BeforeEach
    void prepararCarta() {
        ClienteDAO clienteDAO = new ClienteDAOMemoria();
        new GestorClientes(clienteDAO, new ReservaDAOTxt(tempDir.resolve("reservas.txt")),
                new CompraCandyDAOMemoria()).registrar("Andrei", "andrei@uade.edu.ar");
        directorioTickets = tempDir.resolve("tickets");

        candy = new GestorCandy(new ProductoDAOMemoria(), new CompraCandyDAOMemoria(), clienteDAO,
                new GeneradorTicketCandyTxt(directorioTickets));
        pochoclos = candy.agregarProducto("Pochoclos grandes", TipoProducto.POCHOCLOS, 4000).getId();
        gaseosa = candy.agregarProducto("Gaseosa 500ml", TipoProducto.BEBIDA, 2500).getId();
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
                () -> candy.armarCombo("Combo caro", 6500, pochoclosYGaseosa()));
        assertThrows(IllegalArgumentException.class,
                () -> candy.armarCombo("Combo carísimo", 7000, pochoclosYGaseosa()));
    }

    @Test
    void armaElComboYGuardaQueTrae() {
        Producto combo = candy.armarCombo("Combo pareja", 5500, pochoclosYGaseosa());

        assertTrue(combo.esCombo());
        assertEquals(2, combo.getComponentes().size());
        assertEquals(TipoProducto.COMBO, combo.getTipo());
    }

    @Test
    void unComboNecesitaAlMenosDosProductos() {
        assertThrows(IllegalArgumentException.class,
                () -> candy.armarCombo("Combo de uno", 3000, pedido(pochoclos, 1)));
    }

    @Test
    void unComboNoPuedeContenerOtroCombo() {
        int combo = candy.armarCombo("Combo pareja", 5500, pochoclosYGaseosa()).getId();

        Map<Integer, Integer> anidado = pedido(combo, 1);
        anidado.put(gaseosa, 1);
        assertThrows(IllegalArgumentException.class,
                () -> candy.armarCombo("Combo del combo", 6000, anidado));
    }

    @Test
    void elAhorroEsLaDiferenciaContraComprarloSuelto() {
        int combo = candy.armarCombo("Combo pareja", 5500, pochoclosYGaseosa()).getId();

        CompraCandy compra = candy.vender(1, pedido(combo, 2), MedioPago.EFECTIVO, "");

        assertEquals(11000.0, compra.getTotal(), 0.001);
        assertEquals(2000.0, candy.ahorroDe(compra), 0.001, "6500 sueltos contra 5500, por dos combos");
    }

    @Test
    void elTotalSaleDeLaCartaYNoDeQuienVende() {
        CompraCandy compra = candy.vender(1, pochoclosYGaseosa(), MedioPago.EFECTIVO, "");

        assertEquals(6500.0, compra.getTotal(), 0.001);
        assertEquals(2, compra.getItems().size());
    }

    @Test
    void noSeVendeUnProductoQueSeSacoDeLaCarta() {
        candy.cambiarDisponibilidad(gaseosa, false);

        assertThrows(IllegalArgumentException.class,
                () -> candy.vender(1, pedido(gaseosa, 1), MedioPago.EFECTIVO, ""));
        assertEquals(1, candy.listarDisponibles().size());
        assertEquals(2, candy.listar().size(), "sacarlo de la carta no lo borra");
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
        int combo = candy.armarCombo("Combo pareja", 5500, pochoclosYGaseosa()).getId();
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
        assertEquals(10500.0, candy.totalVendido(LocalDate.now()), 0.001);
    }

    @Test
    void rechazaProductoRepetidoOPrecioInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> candy.agregarProducto("pochoclos grandes", TipoProducto.POCHOCLOS, 4000));
        assertThrows(IllegalArgumentException.class,
                () -> candy.agregarProducto("Agua", TipoProducto.BEBIDA, 0));
    }

    @Test
    void unComboNoSeDaDeAltaComoProductoSuelto() {
        assertThrows(IllegalArgumentException.class,
                () -> candy.agregarProducto("Combo trucho", TipoProducto.COMBO, 5000));
    }
}
