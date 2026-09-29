package ar.uade.cine.model.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.MedioPago;

// Sin Spring ni base: la compra y sus renglones se validan al construirse, así que se prueban con new.
class CompraCandyTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 13, 20, 0);

    private final Producto pochoclos = new Producto("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(4000));
    private final Producto gaseosa = new Producto("Gaseosa", TipoProducto.BEBIDA, Dinero.de(2500));

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, accion).getMessage());
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
        rechaza("El pago con CREDITO necesita código de autorización",
                () -> comprar(MedioPago.CREDITO, "  ", Map.of(pochoclos, 1)));

        assertEquals("AUT-77", comprar(MedioPago.DEBITO, " AUT-77 ", Map.of(pochoclos, 1)).getCodigoAutorizacion());
    }

    @Test
    void cadaRenglonTieneCantidadYUnProductoDisponible() {
        Map<Producto, Integer> sinCantidad = new LinkedHashMap<>();
        sinCantidad.put(pochoclos, null);
        gaseosa.setDisponible(false);

        rechaza("Falta la cantidad de Pochoclos", () -> comprar(MedioPago.EFECTIVO, "", sinCantidad));
        rechaza("La cantidad de Pochoclos debe ser mayor a cero",
                () -> comprar(MedioPago.EFECTIVO, "", Map.of(pochoclos, 0)));
        rechaza("Gaseosa no está disponible", () -> comprar(MedioPago.EFECTIVO, "", Map.of(gaseosa, 1)));
    }

    @Test
    void elRenglonCongelaPrecioYAhorroDelMomentoDeLaVenta() {
        Producto combo = Producto.armarCombo("Combo pareja", Dinero.de(5500), Map.of(pochoclos, 1, gaseosa, 1));
        CompraCandy compra = comprar(MedioPago.EFECTIVO, "", Map.of(combo, 2));

        combo.editar("Combo pareja", Dinero.de(6000));

        assertEquals(Dinero.de(11000), compra.getTotal());
        assertEquals(Dinero.de(2000), compra.getAhorro(), "6500 sueltos contra 5500, por dos combos");
    }
}
