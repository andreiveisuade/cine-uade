package ar.uade.cine.model.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;

// Sin Spring ni base: los invariantes viven en el producto, así que se prueban con new.
class ProductoTest {

    private final Producto pochoclos = new Producto("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(4000));
    private final Producto gaseosa = new Producto("Gaseosa", TipoProducto.BEBIDA, Dinero.de(2500));

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, accion).getMessage());
    }

    private static Dinero pesos(Double pesos) {
        return pesos == null ? null : Dinero.de(pesos);
    }

    private Producto comboPareja() {
        return Producto.armarCombo("Combo pareja", Dinero.de(5500), Map.of(pochoclos, 1, gaseosa, 1));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin nombre,        ,     BEBIDA, 100, El nombre no puede estar vacío
            nombre en blanco,  '  ', BEBIDA, 100, El nombre no puede estar vacío
            sin tipo,          Agua, ,       100, Falta el tipo de producto
            sin precio,        Agua, BEBIDA,    , Falta el precio
            precio en cero,    Agua, BEBIDA,   0, El precio tiene que ser mayor a cero
            cien millones,     Agua, BEBIDA, 100000000, El precio no puede superar $ 1000000.00
            combo sin nombre,  ,     COMBO,  100, 'Un combo se arma con armarCombo, para que declare qué trae'
            """)
    void unSueltoInvalidoNoSeConstruye(String caso, String nombre, TipoProducto tipo, Double precio,
            String mensaje) {
        rechaza(mensaje, () -> new Producto(nombre, tipo, pesos(precio)));
    }

    @Test
    void elNombreSeGuardaSinEspaciosYSoloLosEspaciosNoCuentanParaElLargo() {
        String sesenta = "x".repeat(60);

        assertEquals("Agua", new Producto("  Agua  ", TipoProducto.BEBIDA, Dinero.de(100)).getNombre());
        assertEquals(sesenta, new Producto(" " + sesenta + " ", TipoProducto.BEBIDA, Dinero.de(100)).getNombre());
        rechaza("El nombre no puede tener más de 60 caracteres",
                () -> new Producto(sesenta + "x", null, Dinero.de(100)));
    }

    @Test
    void elComboArmadoSabeQueTraeYCuantoAhorra() {
        Producto combo = comboPareja();

        assertTrue(combo.esCombo());
        assertTrue(combo.estaDisponible());
        assertEquals(2, combo.getComponentes().size());
        assertEquals(Dinero.de(6500), combo.getPrecioSuelto());
        assertEquals(Dinero.de(1000), combo.getAhorro());
    }

    @Test
    void unComboJuntaAlMenosDosProductosYLosDatosSeRechazanAntes() {
        rechaza("Un combo tiene que juntar al menos dos productos distintos",
                () -> Producto.armarCombo("Combo de uno", Dinero.de(3000), Map.of(pochoclos, 1)));
        rechaza("Un combo tiene que juntar al menos dos productos distintos",
                () -> Producto.armarCombo("Combo vacío", Dinero.de(3000), null));
        rechaza("El nombre no puede estar vacío",
                () -> Producto.armarCombo(" ", Dinero.de(3000), Map.of(pochoclos, 1)));
        rechaza("El precio tiene que ser mayor a cero",
                () -> Producto.armarCombo("Combo gratis", Dinero.CERO, Map.of(pochoclos, 1)));
    }

    @Test
    void cadaComponenteTieneCantidadYNoEsOtroCombo() {
        Map<Producto, Integer> sinCantidad = new LinkedHashMap<>();
        sinCantidad.put(pochoclos, null);
        sinCantidad.put(gaseosa, 1);

        rechaza("La cantidad de Pochoclos en el combo debe ser mayor a cero",
                () -> Producto.armarCombo("Combo", Dinero.de(3000), Map.of(pochoclos, 0, gaseosa, 1)));
        rechaza("La cantidad de Pochoclos en el combo debe ser mayor a cero",
                () -> Producto.armarCombo("Combo", Dinero.de(3000), sinCantidad));
        rechaza("Un combo no puede contener otro combo: Combo pareja",
                () -> Producto.armarCombo("Combo doble", Dinero.de(6000), Map.of(comboPareja(), 1, gaseosa, 1)));
    }

    @ParameterizedTest(name = "a $ {0}")
    @CsvSource({"6500", "7000"})
    void elComboTieneQueSalirMenosQueSusComponentesSueltos(double precio) {
        rechaza("El combo tiene que salir menos que sus componentes sueltos ($ 6500.00)",
                () -> Producto.armarCombo("Combo caro", Dinero.de(precio), Map.of(pochoclos, 1, gaseosa, 1)));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            nombre vacío,   '',        4500, El nombre no puede estar vacío
            sin precio,     Pochoclos,     , Falta el precio
            precio en cero, Pochoclos,    0, El precio tiene que ser mayor a cero
            cien millones,  Pochoclos, 100000000, El precio no puede superar $ 1000000.00
            """)
    void editarPideLoMismoQueElAltaYNoTocaNadaSiRechaza(String caso, String nombre, Double precio,
            String mensaje) {
        rechaza(mensaje, () -> pochoclos.editar(nombre, pesos(precio)));

        assertEquals("Pochoclos", pochoclos.getNombre());
        assertEquals(Dinero.de(4000), pochoclos.getPrecio());
    }

    @Test
    void elComboEditadoNoPuedeDejarDeConvenirYNoTocaNadaSiRechaza() {
        Producto combo = comboPareja();

        rechaza("Con ese precio, el combo Combo XL dejaría de salir menos que sus componentes sueltos ($ 6500.00)",
                () -> combo.editar(" Combo XL ", Dinero.de(7000)));

        assertEquals("Combo pareja", combo.getNombre());
        assertEquals(Dinero.de(5500), combo.getPrecio());
    }

    @Test
    void abaratarUnComponenteDejaAlComboSinConvenir() {
        Producto combo = comboPareja();
        combo.exigirQueSigaConviniendo();

        pochoclos.editar("Pochoclos", Dinero.de(2000));

        rechaza("Con ese precio, el combo Combo pareja dejaría de salir menos que sus componentes sueltos ($ 4500.00)",
                combo::exigirQueSigaConviniendo);
    }
}
