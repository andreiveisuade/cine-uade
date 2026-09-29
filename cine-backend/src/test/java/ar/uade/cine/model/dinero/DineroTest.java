package ar.uade.cine.model.dinero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.TipoTarifa;

class DineroTest {

    @Nested
    @DisplayName("Los errores de double que esta clase viene a eliminar")
    class ErroresDeDouble {

        @Test
        @DisplayName("5250.50 x 1.3 daba 6825.650000000001 en double")
        void elCasoDeLaCalculadoraDePrecios() {
            assertNotEquals(6825.65, 5250.50 * 1.3);

            assertEquals(Dinero.de(6825.65), Dinero.de(5250.50).por(1.3));
        }

        @Test
        @DisplayName("sumar 0,10 diez veces da 1 peso exacto, no 0.9999999999999999")
        void laSumaNoAcumulaError() {
            double conDouble = 0;
            for (int i = 0; i < 10; i++) {
                conDouble += 0.10;
            }
            assertNotEquals(1.0, conDouble);

            List<Dinero> diezMonedas = IntStream.range(0, 10)
                    .mapToObj(i -> Dinero.de(0.10))
                    .toList();
            assertEquals(Dinero.de(1), Dinero.sumar(diezMonedas));
        }

        @Test
        @DisplayName("trescientos cobros con centavos suman exacto: es el arqueo del día")
        void elArqueoDeUnDiaCompletoNoDeriva() {
            List<Dinero> cobros = IntStream.range(0, 300)
                    .mapToObj(i -> Dinero.de(1234.56))
                    .toList();

            assertEquals(Dinero.deCentavos(300L * 123456), Dinero.sumar(cobros));
            assertEquals(370368.00, Dinero.sumar(cobros).aPesos(), 0.0);
        }

        @Test
        @DisplayName("dos importes iguales son iguales, sin epsilon")
        void laIgualdadEsExacta() {
            assertEquals(Dinero.de(0.1).mas(Dinero.de(0.2)), Dinero.de(0.3));
        }
    }

    @Nested
    @DisplayName("Las operaciones del negocio")
    class Operaciones {

        @Test
        void sumaYResta() {
            assertEquals(Dinero.de(15000), Dinero.de(10000).mas(Dinero.de(5000)));
            assertEquals(Dinero.de(5000), Dinero.de(15000).menos(Dinero.de(10000)));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(textBlock = """
                un recargo,    5000, 1.3, 6500
                un descuento,  5000, 0.5, 2500
                """)
        @DisplayName("por() es el multiplicador de sala, de butaca y de tarifa")
        void multiplicaPorUnFactorSinUnidad(String caso, double pesos, double factor, double esperado) {
            assertEquals(Dinero.de(esperado), Dinero.de(pesos).por(factor));
        }

        @Test
        @DisplayName("el 30% off de una promoción")
        void calculaPorcentajes() {
            assertEquals(Dinero.de(3000), Dinero.de(10000).porcentaje(30));
        }

        @Test
        @DisplayName("medio centavo se redondea, porque medio centavo no se cobra")
        void redondeaAlCentavo() {
            assertEquals(Dinero.deCentavos(1), Dinero.de(0.005));
            assertEquals(Dinero.deCentavos(333), Dinero.de(10).por(0.3333));
        }
    }

    @Nested
    @DisplayName("Los topes que protegen al cobro")
    class Topes {

        @Test
        @DisplayName("un descuento de $2000 sobre una entrada de $1500 no la deja en negativo")
        void acotadoAImpideElCobroNegativo() {
            Dinero descuento = Dinero.de(2000);
            Dinero subtotal = Dinero.de(1500);

            assertEquals(subtotal, descuento.acotadoA(subtotal));
            assertEquals(Dinero.CERO, subtotal.menos(descuento.acotadoA(subtotal)));
        }

        @Test
        void acotadoADejaPasarLoQueEstaPorDebajo() {
            assertEquals(Dinero.de(500), Dinero.de(500).acotadoA(Dinero.de(1500)));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(textBlock = """
                lo negativo queda en cero, -80, 0
                lo positivo no cambia,      80, 80
                """)
        void sinBajarDeCeroRecortaLoNegativo(String caso, double pesos, double esperado) {
            assertEquals(Dinero.de(esperado), Dinero.de(pesos).sinBajarDeCero());
        }
    }

    @Nested
    @DisplayName("Lo que se carga a mano como precio o monto")
    class ImporteCargado {

        @ParameterizedTest(name = "{0}")
        @CsvSource(textBlock = """
                sin precio,          ,           Falta el precio
                en cero,             0,          El precio tiene que ser mayor a cero
                negativo,            -1,         El precio tiene que ser mayor a cero
                un centavo de más,   1000000.01, El precio no puede superar $ 1000000.00
                cien millones,       100000000,  El precio no puede superar $ 1000000.00
                """)
        void rechazaLoQueNoSePuedeCobrarNiGuardar(String caso, Double pesos, String mensaje) {
            Dinero importe = pesos == null ? null : Dinero.de(pesos);

            assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                    () -> Dinero.importeValido(importe, "precio")).getMessage());
        }

        @Test
        void elTopeMismoSeAcepta() {
            assertEquals(Dinero.IMPORTE_MAXIMO, Dinero.importeValido(Dinero.IMPORTE_MAXIMO, "precio"));
        }

        // Las columnas de plata son DECIMAL(10,2): la entrada más cara, con el recargo más alto de sala,
        // butaca y tarifa sobre el precio tope, también tiene que entrar, o MySQL la rechaza con un 500.
        @Test
        void elTopeDejaLugarParaLosMultiplicadoresDeLaEntrada() {
            double sala = maximo(Arrays.stream(TipoSala.values()).mapToDouble(TipoSala::getMultiplicadorPrecio));
            double butaca = maximo(Arrays.stream(TipoAsiento.values()).mapToDouble(TipoAsiento::getMultiplicadorPrecio));
            double tarifa = maximo(Arrays.stream(TipoTarifa.values()).mapToDouble(TipoTarifa::getMultiplicadorPrecio));
            Dinero columnaLlena = Dinero.de(99_999_999.99);

            assertFalse(Dinero.IMPORTE_MAXIMO.por(sala).por(butaca).por(tarifa).esMayorQue(columnaLlena));
        }

        private static double maximo(DoubleStream multiplicadores) {
            return multiplicadores.max().orElseThrow();
        }
    }

    @Nested
    @DisplayName("Comparar, que es lo que R15 necesita para elegir la promoción que más descuenta")
    class Comparaciones {

        @ParameterizedTest(name = "{0}")
        @CsvSource(textBlock = """
                el mayor es mayor,       3000, 2000, true
                el menor no es mayor,    2000, 3000, false
                un igual no es mayor,    2000, 2000, false
                """)
        void ordenaPorImporte(String caso, double uno, double otro, boolean esMayor) {
            assertEquals(esMayor, Dinero.de(uno).esMayorQue(Dinero.de(otro)));
        }

        @Test
        @DisplayName("R15: entre varias promociones gana la que más descuenta")
        void laMayorDeVariasEsLaQueGana() {
            List<Dinero> descuentos = List.of(Dinero.de(1200), Dinero.de(3000), Dinero.de(800));

            assertEquals(Dinero.de(3000), descuentos.stream().max(Dinero::compareTo).orElseThrow());
        }

        @Test
        void elCeroEsCero() {
            assertTrue(Dinero.CERO.esCero());
            assertTrue(Dinero.de(0).esCero());
            assertFalse(Dinero.deCentavos(1).esCero());
        }
    }

    @Nested
    @DisplayName("Los bordes: JSON y base de datos")
    class Bordes {

        @Test
        void vuelveAPesosParaSalir() {
            assertEquals(15000.0, Dinero.de(15000).aPesos(), 0.0);
            assertEquals(1234.56, Dinero.de(1234.56).aPesos(), 0.0);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(doubles = {0, 0.01, 1234.56, 99999.99})
        @DisplayName("entrar y salir no cambia el importe")
        void elViajeDeIdaYVueltaEsFiel(double pesos) {
            assertEquals(pesos, Dinero.de(pesos).aPesos(), 0.0);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(textBlock = """
                pesos enteros,  15000,   15000.00
                con centavos,   1234.56, 1234.56
                solo centavos,  0.05,    0.05
                negativo,       -0.05,   -0.05
                """)
        @DisplayName("se escribe con dos decimales, como el comprobante")
        void seImprimeConDosDecimales(String caso, double pesos, String impreso) {
            assertEquals(impreso, Dinero.de(pesos).toString());
        }
    }
}
