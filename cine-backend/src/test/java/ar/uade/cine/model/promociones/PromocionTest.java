package ar.uade.cine.model.promociones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.tiempo.Periodo;
import ar.uade.cine.model.ventas.MedioPago;

// Sin Spring ni base: los invariantes viven en los constructores, así que se prueban con new.
class PromocionTest {

    // Viernes: el día que el gestor le pasa desde el reloj.
    private static final LocalDate HOY = LocalDate.of(2026, 8, 14);
    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);
    private static final CondicionesPromocion AGOSTO = condiciones(DESDE, HASTA, Set.of());

    private static CondicionesPromocion condiciones(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias) {
        return new CondicionesPromocion(desde, hasta, dias, null, null, Set.of());
    }

    private static void rechaza(String mensaje, Executable construir) {
        assertEquals(mensaje, assertThrows(Rechazo.class, construir).getMessage());
    }

    // Factory Method: cada tipo crea su subclase, que se reconoce como ese tipo y describe solo lo suyo.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            PORCENTAJE, ar.uade.cine.model.promociones.PromocionPorcentaje, 30.0,      ,  ,
            MONTO_FIJO, ar.uade.cine.model.promociones.PromocionMontoFijo,      , 500.0,  ,
            NXM,        ar.uade.cine.model.promociones.PromocionNxM,            ,      , 2, 1
            """)
    void cadaTipoCreaSuSubclaseYTomaSoloLoSuyo(TipoPromocion tipo, Class<?> clase, Double porcentaje,
            Double monto, Integer lleva, Integer paga) {
        Promocion promocion = tipo.crear("Promo", new ParametrosPromocion(30.0, 500.0, 2, 1), AGOSTO, HOY);

        assertInstanceOf(clase, promocion);
        assertEquals(tipo, promocion.getTipo());
        assertEquals(new ParametrosPromocion(porcentaje, monto, lleva, paga), promocion.getParametros());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin lleva,                  ,   1,  Falta cuántas entradas lleva
            sin paga,                   2,   ,  Falta cuántas entradas paga
            no paga nada,               1,  0,  Un NxM tiene que cobrar al menos una entrada
            paga negativo,              3,  -1, Un NxM tiene que cobrar al menos una entrada
            lleva lo mismo que paga,    2,  2,  En un NxM hay que llevar más de lo que se paga
            lleva menos de lo que paga, 2,  3,  En un NxM hay que llevar más de lo que se paga
            más que el tope por compra, 11, 10, 'Un NxM tiene que llevar como máximo 10 entradas, el tope de butacas por compra'
            """)
    void unNxMQueNoDescuentaNoSeConstruye(String caso, Integer lleva, Integer paga, String mensaje) {
        ParametrosPromocion nxm = ParametrosPromocion.deNxM(lleva, paga);

        rechaza(mensaje, () -> TipoPromocion.NXM.crear("NxM", nxm, AGOSTO, HOY));
    }

    @Test
    void diezPorNueveTodaviaEntraEnUnaCompra() {
        assertEquals(10, new PromocionNxM("10x9", 10, 9, AGOSTO, HOY).getLleva());
    }

    // De 1 a 99 como dice el mensaje; 99,999 en DECIMAL(5,2) se guardaba como 100 y regalaba la entrada.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin porcentaje,     ,       Falta el porcentaje
            el cero,            0,      El porcentaje tiene que estar entre 1 y 99
            medio por ciento,   0.5,    El porcentaje tiene que estar entre 1 y 99
            el cien,            100,    El porcentaje tiene que estar entre 1 y 99
            casi cien,          99.999, El porcentaje tiene que estar entre 1 y 99
            no es un número,    NaN,    El porcentaje tiene que estar entre 1 y 99
            tres decimales,     12.345, El porcentaje tiene que tener como máximo 2 decimales
            """)
    void unPorcentajeFueraDeRangoNoSeConstruye(String caso, Double porcentaje, String mensaje) {
        rechaza(mensaje, () -> new PromocionPorcentaje("Promo", porcentaje, AGOSTO, HOY));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"1", "12.5", "99"})
    void lasPuntasDelPorcentajeSeAceptan(double porcentaje) {
        assertEquals(porcentaje, new PromocionPorcentaje("Promo", porcentaje, AGOSTO, HOY).getPorcentaje());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin monto,      ,          Falta el monto del descuento
            en cero,        0,         El monto del descuento tiene que ser mayor a cero
            tres decimales, 10.555,    El monto del descuento tiene que tener como máximo 2 decimales
            cien millones,  100000000, El monto del descuento no puede superar $ 1000000.00
            """)
    void unMontoFijoSinMontoOFueraDeTopeNoSeConstruye(String caso, Double monto, String mensaje) {
        rechaza(mensaje, () -> new PromocionMontoFijo("Banco", monto, AGOSTO, HOY));
    }

    @Test
    void loComunLoValidaPromocionParaLasTresClases() {
        // Los mismos textos que el pedido: lo que falta se dice que falta, no que está al revés.
        rechaza("La vigencia tiene que empezar antes de terminar",
                () -> new PromocionPorcentaje("rara", 10.0, condiciones(HASTA, DESDE, Set.of()), HOY));
        rechaza("Falta el inicio de la vigencia",
                () -> new PromocionNxM("2x1", 2, 1, condiciones(null, HASTA, Set.of()), HOY));
        rechaza("Falta el fin de la vigencia",
                () -> new PromocionNxM("2x1", 2, 1, condiciones(DESDE, null, Set.of()), HOY));
        rechaza("Falta el nombre", () -> new PromocionMontoFijo(" ", 500.0, AGOSTO, HOY));
        rechaza("El nombre no puede tener más de 60 caracteres",
                () -> new PromocionMontoFijo("x".repeat(61), 500.0, AGOSTO, HOY));
    }

    // Java obliga a llamar a super() primero: lo común se rechaza antes que lo propio del tipo.
    @Test
    void loComunSaleAntesQueLoPropioDelTipo() {
        rechaza("Falta el nombre", () -> new PromocionPorcentaje(" ", null, AGOSTO, HOY));
    }

    // aplicaA pide desde ≤ hora ≤ hasta: guardada, una franja así no correría nunca.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            termina cuando empieza,   20:00, 20:00
            termina a la medianoche,  20:00, 00:00
            cruza la medianoche,      23:00, 01:00
            """)
    void unaFranjaQueNoEmpiezaAntesDeTerminarNoSeConstruye(String caso, LocalTime desde, LocalTime hasta) {
        CondicionesPromocion franja = new CondicionesPromocion(DESDE, HASTA, Set.of(), desde, hasta, Set.of());

        rechaza("La franja horaria tiene que empezar antes de terminar",
                () -> new PromocionPorcentaje("Trasnoche", 20.0, franja, HOY));
    }

    // Una promoción que no va a correr nunca no se guarda: vencida, o con días que no caen en lo que le
    // queda de vigencia. Hoy es viernes 14.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            terminó ayer,                       2026-07-01, 2026-08-13, ,         La vigencia ya terminó: el fin tiene que ser hoy o después
            un viernes de lunes a miércoles,    2026-08-17, 2026-08-19, FRIDAY,   'Ninguno de los días elegidos cae en lo que queda de la vigencia: elegí otro día o extendé la vigencia'
            el único lunes ya pasó,             2026-08-01, 2026-08-16, MONDAY,   'Ninguno de los días elegidos cae en lo que queda de la vigencia: elegí otro día o extendé la vigencia'
            """)
    void unaPromocionQueNuncaAplicaNoSeConstruye(String caso, LocalDate desde, LocalDate hasta, DayOfWeek dia,
            String mensaje) {
        Set<DayOfWeek> dias = dia == null ? Set.of() : Set.of(dia);

        rechaza(mensaje, () -> new PromocionPorcentaje("Promo", 10.0, condiciones(desde, hasta, dias), HOY));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            termina hoy,                 2026-08-01, 2026-08-14,
            el sábado que queda,         2026-08-01, 2026-08-16, SATURDAY
            empieza en una semana,       2026-08-21, 2026-08-21, FRIDAY
            """)
    void unaPromocionQueTodaviaPuedeAplicarSeConstruye(String caso, LocalDate desde, LocalDate hasta,
            DayOfWeek dia) {
        Set<DayOfWeek> dias = dia == null ? Set.of() : Set.of(dia);

        assertEquals(hasta, new PromocionPorcentaje("Promo", 10.0, condiciones(desde, hasta, dias), HOY)
                .getVigencia().hasta());
    }

    @Test
    void elNombreSeGuardaSinLosEspaciosDeAlrededor() {
        assertEquals("Martes 30%", new PromocionPorcentaje("  Martes 30%  ", 30.0, AGOSTO, HOY).getNombre());
    }

    @Test
    void unaValidaGuardaSusCondiciones() {
        PromocionNxM dosPorUno = new PromocionNxM("2x1", 2, 1, AGOSTO, HOY);

        assertEquals(new Periodo(DESDE, HASTA), dosPorUno.getVigencia());
        assertEquals(2, dosPorUno.getLleva());
        assertEquals(1, dosPorUno.getPaga());
    }

    // Sin horas, la franja es todo el día: aplica a la función de las 10 y a la trasnoche.
    @Test
    void sinFranjaCorreTodoElDia() {
        PromocionNxM dosPorUno = new PromocionNxM("2x1", 2, 1, AGOSTO, HOY);

        assertTrue(dosPorUno.aplicaA(LocalDateTime.of(2026, 8, 20, 10, 0), MedioPago.EFECTIVO));
        assertTrue(dosPorUno.aplicaA(LocalDateTime.of(2026, 8, 20, 23, 59), MedioPago.EFECTIVO));
    }
}
