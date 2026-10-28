package ar.uade.cine.model.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.ventas.MedioPago;

// Sin Spring ni base: la compra y sus renglones se validan al construirse, así que se prueban con new.
class CompraCandyTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 13, 20, 0);

    private final Producto pochoclos = new ProductoSuelto("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(4000));
    private final Producto gaseosa = new ProductoSuelto("Gaseosa", TipoProducto.BEBIDA, Dinero.de(2500));

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(Rechazo.class, accion).getMessage());
    }

    private static CompraCandy comprar(MedioPago medio, String codigo, Map<Producto, Integer> cantidades) {
        return new CompraCandy(null, null, AHORA, medio, codigo, cantidades);
    }

    @Test
    void sinProductosNiMedioDePagoNoSeConstruyeYLosProductosSeRechazanPrimero() {
        rechaza("Hay que elegir al menos un producto", () -> comprar(MedioPago.EFECTIVO, "", Map.of()));
        rechaza("Hay que elegir al menos un producto", () -> comprar(null, "", null));
        rechaza("Falta el medio de pago", () -> comprar(null, "", Map.of(pochoclos, 1)));
    }

    @Test
    void unMedioElectronicoSinCodigoNoSeConstruyeYElCodigoSeGuardaLimpio() {
        rechaza("Falta el código de autorización del pago con crédito",
                () -> comprar(MedioPago.CREDITO, "  ", Map.of(pochoclos, 1)));

        assertEquals("AUT-77", comprar(MedioPago.DEBITO, " AUT-77 ", Map.of(pochoclos, 1)).getCodigoAutorizacion());
    }

    @Test
    void cadaRenglonTieneCantidadYUnProductoDisponible() {
        Map<Producto, Integer> sinCantidad = new LinkedHashMap<>();
        sinCantidad.put(pochoclos, null);
        gaseosa.sacarDeLaVenta();

        rechaza("Falta la cantidad de Pochoclos", () -> comprar(MedioPago.EFECTIVO, "", sinCantidad));
        rechaza("Gaseosa no está disponible", () -> comprar(MedioPago.EFECTIVO, "", Map.of(gaseosa, 1)));
    }

    // Tope de Andrei: más de veinte de lo mismo en un renglón es un error de tipeo, no una venta de mostrador.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            en cero,       0,   La cantidad de Pochoclos tiene que ser mayor a cero
            negativa,      -3,  La cantidad de Pochoclos tiene que ser mayor a cero
            más de veinte, 21,  La cantidad de Pochoclos tiene que ser como máximo 20
            un tipeo,      200, La cantidad de Pochoclos tiene que ser como máximo 20
            """)
    void laCantidadDeUnRenglonVaDeUnoAVeinte(String caso, int cantidad, String mensaje) {
        rechaza(mensaje, () -> comprar(MedioPago.EFECTIVO, "", Map.of(pochoclos, cantidad)));
    }

    @Test
    void veinteDeLoMismoTodaviaSeVenden() {
        assertEquals(Dinero.de(80000), comprar(MedioPago.EFECTIVO, "", Map.of(pochoclos, 20)).getTotal());
    }

    @Test
    void elRenglonCongelaPrecioYAhorroDelMomentoDeLaVenta() {
        Producto combo = Combo.armar("Combo pareja", Dinero.de(5500), Map.of(pochoclos, 1, gaseosa, 1));
        CompraCandy compra = comprar(MedioPago.EFECTIVO, "", Map.of(combo, 2));

        combo.editar("Combo pareja", Dinero.de(6000));

        assertEquals(Dinero.de(11000), compra.getTotal());
        assertEquals(Dinero.de(2000), compra.getAhorro(), "6500 sueltos contra 5500, por dos combos");
    }

    // R30: el pedido de la web no tiene medio hasta que se cobra con su reserva.
    @Test
    void elPedidoDeLaWebNacePendienteYSinMedio() {
        CompraCandy pedido = CompraCandy.pendiente(3, 25, AHORA, Map.of(pochoclos, 2));

        assertEquals(EstadoCompraCandy.PENDIENTE, pedido.getEstado());
        assertNull(pedido.getMedio());
        assertNull(pedido.getCobradaEn());
        assertEquals(Dinero.de(8000), pedido.getTotal());
        rechaza("Hay que elegir al menos un producto", () -> CompraCandy.pendiente(3, 25, AHORA, Map.of()));
    }

    @Test
    void laDeMostradorNaceCobradaEnElMomentoDeLaVenta() {
        CompraCandy compra = comprar(MedioPago.EFECTIVO, "", Map.of(pochoclos, 1));

        assertEquals(EstadoCompraCandy.COBRADA, compra.getEstado());
        assertEquals(AHORA, compra.getCobradaEn());
    }

    @Test
    void cobrarElPedidoLeDaElMedioYLaHoraDelPago() {
        CompraCandy pedido = CompraCandy.pendiente(3, 25, AHORA, Map.of(pochoclos, 1));

        pedido.cobrar(MedioPago.DEBITO, " MP-1 ", AHORA.plusMinutes(10));

        assertEquals(EstadoCompraCandy.COBRADA, pedido.getEstado());
        assertEquals(MedioPago.DEBITO, pedido.getMedio());
        assertEquals("MP-1", pedido.getCodigoAutorizacion());
        assertEquals(AHORA.plusMinutes(10), pedido.getCobradaEn());
        assertEquals(AHORA, pedido.getFecha(), "la fecha sigue siendo la del pedido");
    }

    // Patrón State: cada estado dice qué deja hacer, como EstadoReserva.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            cobrar lo cobrado  | true  | cobrar | La compra de candy está cobrada: no se puede cobrar
            anular lo cobrado  | true  | anular | La compra de candy está cobrada: solo se anula un pedido sin cobrar
            cobrar lo anulado  | false | cobrar | La compra de candy está anulada: no se puede cobrar
            anular lo anulado  | false | anular | La compra de candy está anulada: solo se anula un pedido sin cobrar
            """)
    void loQueYaNoEstaPendienteNoSeCobraNiSeAnula(String caso, boolean cobrada, String accion, String mensaje) {
        CompraCandy pedido = CompraCandy.pendiente(3, 25, AHORA, Map.of(pochoclos, 1));
        if (cobrada) {
            pedido.cobrar(MedioPago.EFECTIVO, "", AHORA);
        } else {
            pedido.anular();
        }

        rechaza(mensaje, () -> {
            if (accion.equals("cobrar")) {
                pedido.cobrar(MedioPago.EFECTIVO, "", AHORA);
            } else {
                pedido.anular();
            }
        });
    }

    @Test
    void elCobroValidaElMedioComoElMostrador() {
        CompraCandy pedido = CompraCandy.pendiente(3, 25, AHORA, Map.of(pochoclos, 1));

        rechaza("Falta el medio de pago", () -> pedido.cobrar(null, "", AHORA));
        rechaza("Falta el código de autorización del pago con QR", () -> pedido.cobrar(MedioPago.QR, "", AHORA));
        assertEquals(EstadoCompraCandy.PENDIENTE, pedido.getEstado(), "un cobro rechazado no cambia el estado");
    }
}
