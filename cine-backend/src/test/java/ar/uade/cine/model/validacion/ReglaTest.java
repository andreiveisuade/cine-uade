package ar.uade.cine.model.validacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class ReglaTest {

    private static void rechaza(String mensaje, Runnable guarda) {
        DatoInvalido error = assertThrows(DatoInvalido.class, guarda::run);
        assertEquals(mensaje, error.getMessage());
    }

    @Nested
    @DisplayName("Un texto")
    class Texto {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void obligatorioRechazaElNullYElBlanco(String valor) {
            rechaza("El título no puede estar vacío",
                    () -> Regla.texto(valor).obligatorio("El título no puede estar vacío"));
        }

        @Test
        void recortadoLeSacaLosEspaciosDeLasPuntas() {
            assertEquals("Matrix", Regla.texto("  Matrix ").recortado().valor());
        }

        // El largo se mide ya recortado: los espacios de más no cuentan contra la columna.
        @Test
        void hastaMideElLargoYNombraElMaximo() {
            String cien = "x".repeat(100);

            assertEquals(cien, Regla.texto(" " + cien + " ").recortado().hasta(100, "El título").valor());
            rechaza("El título no puede tener más de 100 caracteres",
                    () -> Regla.texto(cien + "x").hasta(100, "El título"));
        }

        @Test
        void todoEncadenadoDevuelveElTextoLimpio() {
            String titulo = Regla.texto(" Dune ").obligatorio("El título no puede estar vacío")
                    .recortado().hasta(100, "El título").valor();

            assertEquals("Dune", titulo);
        }

        // Un opcional (el director) se valida solo si vino: que tenga que venir lo dice obligatorio().
        @Test
        void unNullQueNoEsObligatorioPasaYSigueNull() {
            assertNull(Regla.texto(null).recortado().hasta(100, "El director").valor());
        }
    }

    @Nested
    @DisplayName("Un número")
    class Numero {

        @Test
        void obligatorioRechazaElNull() {
            rechaza("Falta la duración", () -> Regla.numero((Integer) null).obligatorio("Falta la duración"));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -5})
        void mayorQueCeroRechazaElCeroYLosNegativos(int minutos) {
            rechaza("La duración tiene que ser mayor a cero",
                    () -> Regla.numero(minutos).mayorQueCero("La duración tiene que ser mayor a cero"));
        }

        @Test
        void noNegativoAceptaElCeroYRechazaLoQueEstaDebajo() {
            assertEquals(0, Regla.numero(0).noNegativo("Los votos no pueden ser negativos").valor());
            rechaza("Los votos no pueden ser negativos",
                    () -> Regla.numero(-1).noNegativo("Los votos no pueden ser negativos"));
        }

        @Test
        void entreIncluyeLasDosPuntas() {
            String mensaje = "El porcentaje tiene que estar entre 1 y 99";

            assertEquals(1, Regla.numero(1).entre(1, 99, mensaje).valor());
            assertEquals(99, Regla.numero(99).entre(1, 99, mensaje).valor());
            rechaza(mensaje, () -> Regla.numero(0).entre(1, 99, mensaje));
            rechaza(mensaje, () -> Regla.numero(100).entre(1, 99, mensaje));
        }

        // El puntaje es decimal: las mismas guardas sirven para cualquier número.
        @Test
        void sirveParaDecimales() {
            String mensaje = "El puntaje tiene que estar entre 0 y 10";

            assertEquals(8.2, Regla.numero(8.2).entre(0.0, 10.0, mensaje).valor());
            rechaza(mensaje, () -> Regla.numero(10.1).entre(0.0, 10.0, mensaje));
            rechaza("El precio tiene que ser mayor a cero",
                    () -> Regla.numero(0.0).mayorQueCero("El precio tiene que ser mayor a cero"));
        }

        // Jackson convierte "NaN" e "Infinity" en un Double: ninguna guarda de número los deja pasar.
        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void niNaNNiInfinitoPasanPorNingunaGuarda(double raro) {
            rechaza("m", () -> Regla.numero(raro).mayorQueCero("m"));
            rechaza("m", () -> Regla.numero(raro).noNegativo("m"));
            rechaza("m", () -> Regla.numero(raro).entre(0.0, 10.0, "m"));
            rechaza("m", () -> Regla.numero(raro).conDecimales(2, "m"));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(textBlock = """
                sin decimales,  12,      true
                dos,            99.99,   true
                cero al final,  10.50,   true
                tres,           99.999,  false
                """)
        void conDecimalesCuentaLosQueTieneElNumero(String caso, double valor, boolean pasa) {
            Runnable guarda = () -> Regla.numero(valor).conDecimales(2, "Como máximo 2 decimales");
            if (pasa) {
                guarda.run();
            } else {
                rechaza("Como máximo 2 decimales", guarda);
            }
        }

        @Test
        void todoEncadenadoDevuelveElNumero() {
            int duracion = Regla.numero(155).obligatorio("Falta la duración")
                    .mayorQueCero("La duración tiene que ser mayor a cero").valor();

            assertEquals(155, duracion);
        }

        @Test
        void unNullQueNoEsObligatorioPasaSinCompararse() {
            assertNull(Regla.numero((Integer) null).mayorQueCero("x").noNegativo("x").entre(1, 3, "x").valor());
        }
    }

    @Nested
    @DisplayName("Un objeto")
    class Objeto {

        @Test
        void obligatorioRechazaElNullYDevuelveElValor() {
            LocalDate hoy = LocalDate.of(2026, 9, 29);

            assertEquals(hoy, Regla.objeto(hoy).obligatorio("Falta la fecha de inicio").valor());
            rechaza("Falta la fecha de inicio",
                    () -> Regla.objeto((LocalDate) null).obligatorio("Falta la fecha de inicio"));
        }
    }

    @Nested
    @DisplayName("Una lista")
    class Lista {

        @ParameterizedTest
        @NullSource
        void noVaciaRechazaElNullYLaVacia(List<String> nula) {
            rechaza("Falta elegir una butaca", () -> Regla.lista(nula).noVacia("Falta elegir una butaca"));
            rechaza("Falta elegir una butaca", () -> Regla.lista(List.of()).noVacia("Falta elegir una butaca"));
        }

        @Test
        void sinNulosRechazaUnElementoNullAunqueLaListaSeaInmutable() {
            rechaza("Hay un género vacío", () -> Regla.lista(Arrays.asList("DRAMA", null)).sinNulos("Hay un género vacío"));
            assertEquals(List.of("DRAMA"), Regla.lista(List.of("DRAMA")).sinNulos("Hay un género vacío").valor());
        }

        @Test
        void hastaIncluyeElTope() {
            assertEquals(2, Regla.lista(List.of("A1", "A2")).hasta(2, "Hasta 2").valor().size());
            rechaza("Hasta 2", () -> Regla.lista(List.of("A1", "A2", "A3")).hasta(2, "Hasta 2"));
        }
    }
}
