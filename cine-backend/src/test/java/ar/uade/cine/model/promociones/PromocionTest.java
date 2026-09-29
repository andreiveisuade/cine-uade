package ar.uade.cine.model.promociones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;

// Sin Spring ni base: los invariantes viven en los constructores, así que se prueban con new.
class PromocionTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);
    private static final CondicionesPromocion AGOSTO =
            new CondicionesPromocion(DESDE, HASTA, Set.of(), null, null, Set.of());

    private static void rechaza(String mensaje, Executable construir) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, construir).getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            lleva lo mismo que paga,    2x2, 2, 2
            lleva menos de lo que paga, 2x3, 2, 3
            no paga nada,               1x0, 1, 0
            """)
    void unNxMQueNoDescuentaNoSeConstruye(String caso, String nombre, int lleva, int paga) {
        rechaza("En un NxM hay que llevar más de lo que se paga", () -> new PromocionNxM(nombre, lleva, paga, AGOSTO));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            el cien por ciento, gratis, 100
            el cero por ciento, nada,   0
            """)
    void unPorcentajeFueraDeRangoNoSeConstruye(String caso, String nombre, int porcentaje) {
        rechaza("El porcentaje tiene que estar entre 1 y 99", () -> new PromocionPorcentaje(nombre, porcentaje, AGOSTO));
    }

    @Test
    void unMontoFijoSinMontoNoSeConstruye() {
        rechaza("El monto del descuento debe ser mayor a cero", () -> new PromocionMontoFijo("cero", Dinero.CERO, AGOSTO));
        rechaza("El monto del descuento debe ser mayor a cero", () -> new PromocionMontoFijo("nulo", null, AGOSTO));
    }

    @Test
    void loComunLoValidaPromocionParaLasTresClases() {
        CondicionesPromocion alReves = new CondicionesPromocion(HASTA, DESDE, Set.of(), null, null, Set.of());
        CondicionesPromocion sinFin = new CondicionesPromocion(DESDE, null, Set.of(), null, null, Set.of());

        rechaza("La vigencia tiene que empezar antes de terminar", () -> new PromocionPorcentaje("rara", 10, alReves));
        rechaza("La vigencia tiene que empezar antes de terminar", () -> new PromocionNxM("2x1", 2, 1, sinFin));
        rechaza("La promoción necesita un nombre", () -> new PromocionMontoFijo(" ", Dinero.de(500), AGOSTO));
    }

    @Test
    void unaValidaGuardaSusCondiciones() {
        PromocionNxM dosPorUno = new PromocionNxM("2x1", 2, 1, AGOSTO);

        assertEquals(DESDE, dosPorUno.getVigenciaDesde());
        assertEquals(HASTA, dosPorUno.getVigenciaHasta());
        assertEquals(2, dosPorUno.getLleva());
        assertEquals(1, dosPorUno.getPaga());
    }
}
