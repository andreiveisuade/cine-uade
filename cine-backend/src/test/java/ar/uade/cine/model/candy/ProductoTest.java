package ar.uade.cine.model.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.Rechazo;

// Sin Spring ni base: los invariantes viven en el producto, así que se prueban con new.
class ProductoTest {

    private final Producto pochoclos = new ProductoSuelto("Pochoclos", TipoProducto.POCHOCLOS, Dinero.de(4000));
    private final Producto gaseosa = new ProductoSuelto("Gaseosa", TipoProducto.BEBIDA, Dinero.de(2500));

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(Rechazo.class, accion).getMessage());
    }

    private static Dinero pesos(Double pesos) {
        return pesos == null ? null : Dinero.de(pesos);
    }

    private Combo comboPareja() {
        return Combo.armar("Combo pareja", Dinero.de(5500), Map.of(pochoclos, 1, gaseosa, 1));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin nombre,        ,     BEBIDA, 100, Falta el nombre
            nombre en blanco,  '  ', BEBIDA, 100, Falta el nombre
            sin tipo,          Agua, ,       100, Falta el tipo de producto
            sin precio,        Agua, BEBIDA,    , Falta el precio
            precio en cero,    Agua, BEBIDA,   0, El precio tiene que ser mayor a cero
            cien millones,     Agua, BEBIDA, 100000000, El precio no puede superar $ 1000000.00
            combo sin nombre,  ,     COMBO,  100, 'Un combo se da de alta como combo: con sus componentes'
            """)
    void unSueltoInvalidoNoSeConstruye(String caso, String nombre, TipoProducto tipo, Double precio,
            String mensaje) {
        rechaza(mensaje, () -> new ProductoSuelto(nombre, tipo, pesos(precio)));
    }

    @Test
    void elNombreSeGuardaSinEspaciosYSoloLosEspaciosNoCuentanParaElLargo() {
        String sesenta = "x".repeat(60);

        assertEquals("Agua", new ProductoSuelto("  Agua  ", TipoProducto.BEBIDA, Dinero.de(100)).getNombre());
        assertEquals(sesenta, new ProductoSuelto(" " + sesenta + " ", TipoProducto.BEBIDA, Dinero.de(100)).getNombre());
        rechaza("El nombre no puede tener más de 60 caracteres",
                () -> new ProductoSuelto(sesenta + "x", null, Dinero.de(100)));
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

    // Composite: quien cobra pregunta a un Producto sin saber si es hoja o combo, y cada uno contesta lo suyo.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            un suelto vale su precio,     false, 4000, 0
            un combo vale sus partes,     true,  6500, 1000
            """)
    void cualquierProductoContestaSuPrecioSueltoYSuAhorro(String caso, boolean esCombo, double precioSuelto,
            double ahorro) {
        Producto producto = esCombo ? comboPareja() : pochoclos;

        assertEquals(Dinero.de(precioSuelto), producto.getPrecioSuelto());
        assertEquals(Dinero.de(ahorro), producto.getAhorro());
    }

    @Test
    void unComboJuntaAlMenosDosProductosYLosDatosSeRechazanAntes() {
        rechaza("Un combo tiene que juntar al menos dos productos distintos",
                () -> Combo.armar("Combo de uno", Dinero.de(3000), Map.of(pochoclos, 1)));
        rechaza("Un combo tiene que juntar al menos dos productos distintos",
                () -> Combo.armar("Combo vacío", Dinero.de(3000), null));
        rechaza("Falta el nombre",
                () -> Combo.armar(" ", Dinero.de(3000), Map.of(pochoclos, 1)));
        rechaza("El precio tiene que ser mayor a cero",
                () -> Combo.armar("Combo gratis", Dinero.CERO, Map.of(pochoclos, 1)));
    }

    @Test
    void cadaComponenteTieneCantidadYNoEsOtroCombo() {
        Map<Producto, Integer> sinCantidad = new LinkedHashMap<>();
        sinCantidad.put(pochoclos, null);
        sinCantidad.put(gaseosa, 1);

        rechaza("Falta la cantidad de Pochoclos",
                () -> Combo.armar("Combo", Dinero.de(3000), sinCantidad));
        rechaza("Un combo no puede contener otro combo: Combo pareja",
                () -> Combo.armar("Combo doble", Dinero.de(6000), Map.of(comboPareja(), 1, gaseosa, 1)));
    }

    // Los mismos textos que un renglón de la venta (CompraCandyTest): la cantidad es la misma cosa en los dos.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            en cero,      0,  La cantidad de Pochoclos tiene que ser mayor a cero
            negativa,     -1, La cantidad de Pochoclos tiene que ser mayor a cero
            más de veinte, 21, La cantidad de Pochoclos tiene que ser como máximo 20
            """)
    void laCantidadDeUnComponenteVaDeUnoAVeinte(String caso, int cantidad, String mensaje) {
        rechaza(mensaje, () -> Combo.armar("Combo", Dinero.de(3000), Map.of(pochoclos, cantidad, gaseosa, 1)));
    }

    @Test
    void veinteDeUnComponenteTodaviaEsUnCombo() {
        Producto combo = Combo.armar("Combo gigante", Dinero.de(80000), Map.of(pochoclos, 20, gaseosa, 1));

        assertEquals(20, combo.getComponentes().stream().mapToInt(ItemCombo::cantidad).max().orElseThrow());
    }

    @ParameterizedTest(name = "a $ {0}")
    @CsvSource({"6500", "7000"})
    @DisplayName("R14: el combo tiene que salir menos que sus componentes")
    void elComboTieneQueSalirMenosQueSusComponentesSueltos(double precio) {
        rechaza("El combo tiene que salir menos que sus componentes sueltos ($ 6500.00)",
                () -> Combo.armar("Combo caro", Dinero.de(precio), Map.of(pochoclos, 1, gaseosa, 1)));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            nombre vacío,   '',        4500, Falta el nombre
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
    @DisplayName("R14: el combo editado tiene que seguir conviniendo")
    void elComboEditadoNoPuedeDejarDeConvenirYNoTocaNadaSiRechaza() {
        Producto combo = comboPareja();

        rechaza("Con ese precio, el combo Combo XL dejaría de salir menos que sus componentes sueltos ($ 6500.00)",
                () -> combo.editar(" Combo XL ", Dinero.de(7000)));

        assertEquals("Combo pareja", combo.getNombre());
        assertEquals(Dinero.de(5500), combo.getPrecio());
    }

    @Test
    @DisplayName("R14: abaratar un componente deja al combo sin convenir")
    void abaratarUnComponenteDejaAlComboSinConvenir() {
        Combo combo = comboPareja();
        combo.exigirQueSigaConviniendo();

        pochoclos.editar("Pochoclos", Dinero.de(2000));

        rechaza("Con ese precio, el combo Combo pareja dejaría de salir menos que sus componentes sueltos ($ 4500.00)",
                combo::exigirQueSigaConviniendo);
    }
}
