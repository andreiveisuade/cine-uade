package ar.uade.cine.dominio.dinero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Lo que {@link Dinero} promete en su javadoc, escrito de manera que no pueda mentir.
 *
 * <p>Cada caso de la primera sección es un cálculo que con {@code double} daba mal. No son
 * ejemplos inventados: salen de los comentarios que había en {@code CalculadoraPrecio} y
 * en el arqueo, donde el problema estaba descrito en prosa y sin nada que lo verificara.
 */
class DineroTest {

    @Nested
    @DisplayName("Los errores de double que esta clase viene a eliminar")
    class ErroresDeDouble {

        @Test
        @DisplayName("5250.50 x 1.3 daba 6825.650000000001 en double")
        void elCasoDeLaCalculadoraDePrecios() {
            // Lo que pasaba antes, y que obligó a redondear después de cada cuenta.
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
            // 300 cobros de $1234,56. Con double, el total se despega de a poco; acá no.
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

        @Test
        @DisplayName("por() es el multiplicador de sala, de butaca y de tarifa")
        void multiplicaPorUnFactorSinUnidad() {
            assertEquals(Dinero.de(6500), Dinero.de(5000).por(1.3));
            // Un jubilado paga la mitad.
            assertEquals(Dinero.de(2500), Dinero.de(5000).por(0.5));
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

        @Test
        void sinBajarDeCeroRecortaLoNegativo() {
            assertEquals(Dinero.CERO, Dinero.de(-80).sinBajarDeCero());
            assertEquals(Dinero.de(80), Dinero.de(80).sinBajarDeCero());
        }
    }

    @Nested
    @DisplayName("Comparar, que es lo que R15 necesita para elegir la promoción que más descuenta")
    class Comparaciones {

        @Test
        void ordenaPorImporte() {
            assertTrue(Dinero.de(3000).esMayorQue(Dinero.de(2000)));
            assertFalse(Dinero.de(2000).esMayorQue(Dinero.de(3000)));
            assertFalse(Dinero.de(2000).esMayorQue(Dinero.de(2000)));
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

        @Test
        @DisplayName("entrar y salir no cambia el importe")
        void elViajeDeIdaYVueltaEsFiel() {
            for (double pesos : new double[] {0, 0.01, 1234.56, 99999.99}) {
                assertEquals(pesos, Dinero.de(pesos).aPesos(), 0.0);
            }
        }

        @Test
        @DisplayName("se escribe con dos decimales, como el comprobante")
        void seImprimeConDosDecimales() {
            assertEquals("15000.00", Dinero.de(15000).toString());
            assertEquals("1234.56", Dinero.de(1234.56).toString());
            assertEquals("0.05", Dinero.de(0.05).toString());
            assertEquals("-0.05", Dinero.de(-0.05).toString());
        }
    }
}
