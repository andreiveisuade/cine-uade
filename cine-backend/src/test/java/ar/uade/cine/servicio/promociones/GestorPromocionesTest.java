package ar.uade.cine.servicio.promociones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.uade.cine.dominio.promociones.Promocion;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.memoria.PromocionDAOMemoria;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * R15 (las promociones no se acumulan: gana la que más descuenta) y R16 (las entradas de
 * tarifa reducida no participan del descuento), más el cálculo de cada tipo.
 */
class GestorPromocionesTest {

    /** 20 de agosto de 2026 es jueves; el 19, miércoles. */
    private static final LocalDateTime JUEVES = LocalDateTime.of(2026, 8, 20, 20, 0);
    private static final LocalDateTime MIERCOLES = LocalDateTime.of(2026, 8, 19, 20, 0);
    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);

    private GestorPromociones promociones;

    @BeforeEach
    void prepararCarta() {
        promociones = new GestorPromociones(new PromocionDAOMemoria());
    }

    private static Entrada entrada(double precio) {
        // el literal entra en pesos y se convierte una sola vez, como en el resto del sistema
        return new Entrada(1, "A1", TipoTarifa.GENERAL, Dinero.de(precio));
    }

    private static Entrada entrada(double precio, TipoTarifa tarifa) {
        return new Entrada(1, "A1", tarifa, Dinero.de(precio));
    }

    // ---------- cada tipo calcula lo suyo ----------

    @Test
    void elPorcentajeDescuentaSobreElSubtotal() {
        Promocion promo = promociones.crearPorcentaje("30 off", 30, DESDE, HASTA,
                Set.of(), null, null, Set.of());

        assertEquals(Dinero.de(3000), promo.calcularDescuento(List.of(entrada(5000), entrada(5000))));
    }

    /** Descontar más que el total dejaría un cobro negativo. */
    @Test
    void elMontoFijoNuncaDescuentaMasQueElTotal() {
        Promocion promo = promociones.crearMontoFijo("2000 off", Dinero.de(2000), DESDE, HASTA,
                Set.of(), null, null, Set.of());

        assertEquals(Dinero.de(1500), promo.calcularDescuento(List.of(entrada(1500))));
    }

    /** El 2x1 regala la más barata, que es lo que hace cualquier cine. */
    @Test
    void elDosPorUnoRegalaLaMasBarata() {
        Promocion promo = promociones.crearNxM("2x1", 2, 1, DESDE, HASTA,
                Set.of(), null, null, Set.of());

        assertEquals(Dinero.de(4000), promo.calcularDescuento(List.of(entrada(6000), entrada(4000))));
    }

    /** Con tres entradas entra un solo grupo de dos: la tercera se paga entera. */
    @Test
    void elDosPorUnoSoloCuentaGruposCompletos() {
        Promocion promo = promociones.crearNxM("2x1", 2, 1, DESDE, HASTA,
                Set.of(), null, null, Set.of());

        assertEquals(Dinero.de(5000), promo.calcularDescuento(
                List.of(entrada(5000), entrada(5000), entrada(5000))));
    }

    @Test
    void unaSolaEntradaNoActivaElDosPorUno() {
        Promocion promo = promociones.crearNxM("2x1", 2, 1, DESDE, HASTA,
                Set.of(), null, null, Set.of());

        assertEquals(Dinero.de(0), promo.calcularDescuento(List.of(entrada(5000))));
    }

    // ---------- R15: no se acumulan, gana la mejor ----------

    @Test
    void ganaLaQueMasDescuenta() {
        promociones.crearPorcentaje("10 off", 10, DESDE, HASTA, Set.of(), null, null, Set.of());
        promociones.crearNxM("2x1", 2, 1, DESDE, HASTA, Set.of(), null, null, Set.of());

        List<Entrada> dos = List.of(entrada(5000), entrada(5000));
        Optional<Promocion> ganadora = promociones.mejorPara(dos, JUEVES, MedioPago.EFECTIVO);

        assertTrue(ganadora.isPresent());
        assertEquals("2x1", ganadora.get().getNombre(), "el 2x1 saca 5000 y el 10% solo 1000");
        assertEquals(Dinero.de(5000), promociones.descuentoDe(ganadora.get(), dos));
    }

    /** Arbitrario pero determinístico: dos cobros iguales tienen que dar lo mismo. */
    @Test
    void enUnEmpateGanaLaDeMenorId() {
        promociones.crearPorcentaje("primera", 20, DESDE, HASTA, Set.of(), null, null, Set.of());
        promociones.crearPorcentaje("segunda", 20, DESDE, HASTA, Set.of(), null, null, Set.of());

        Optional<Promocion> ganadora = promociones.mejorPara(
                List.of(entrada(5000)), JUEVES, MedioPago.EFECTIVO);

        assertEquals("primera", ganadora.orElseThrow().getNombre());
    }

    // ---------- R16: la tarifa reducida queda afuera ----------

    @Test
    void laTarifaReducidaNoParticipaDelDescuento() {
        Promocion promo = promociones.crearPorcentaje("50 off", 50, DESDE, HASTA,
                Set.of(), null, null, Set.of());

        List<Entrada> mixta = List.of(entrada(5000), entrada(2500, TipoTarifa.JUBILADO));

        assertEquals(Dinero.de(2500), promociones.descuentoDe(promo, mixta),
                "el 50% corre solo sobre los 5000 de la general");
    }

    @Test
    void sinEntradasGeneralesNoAplicaNinguna() {
        promociones.crearPorcentaje("50 off", 50, DESDE, HASTA, Set.of(), null, null, Set.of());

        assertTrue(promociones.mejorPara(List.of(entrada(2500, TipoTarifa.JUBILADO)),
                JUEVES, MedioPago.EFECTIVO).isEmpty());
    }

    // ---------- condiciones ----------

    @Test
    void laDelMiercolesNoCorreUnJueves() {
        promociones.crearNxM("Miércoles 2x1", 2, 1, DESDE, HASTA,
                Set.of(DayOfWeek.WEDNESDAY), null, null, Set.of());

        List<Entrada> dos = List.of(entrada(5000), entrada(5000));

        assertTrue(promociones.mejorPara(dos, MIERCOLES, MedioPago.EFECTIVO).isPresent());
        assertTrue(promociones.mejorPara(dos, JUEVES, MedioPago.EFECTIVO).isEmpty());
    }

    /** Es la razón por la que el descuento no se puede resolver al reservar. */
    @Test
    void laDelBancoSoloCorreConEseMedioDePago() {
        promociones.crearMontoFijo("Banco", Dinero.de(1000), DESDE, HASTA,
                Set.of(), null, null, Set.of(MedioPago.CREDITO));

        List<Entrada> una = List.of(entrada(5000));

        assertTrue(promociones.mejorPara(una, JUEVES, MedioPago.CREDITO).isPresent());
        assertTrue(promociones.mejorPara(una, JUEVES, MedioPago.EFECTIVO).isEmpty());
    }

    @Test
    void fueraDeVigenciaNoCorre() {
        promociones.crearPorcentaje("Julio", 30, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31), Set.of(), null, null, Set.of());

        assertTrue(promociones.mejorPara(List.of(entrada(5000)), JUEVES, MedioPago.EFECTIVO).isEmpty());
    }

    @Test
    void laTrasnocheNoCorreEnLaFuncionDeLaTarde() {
        promociones.crearPorcentaje("Trasnoche", 40, DESDE, HASTA,
                Set.of(), LocalTime.of(23, 0), null, Set.of());

        assertTrue(promociones.mejorPara(List.of(entrada(5000)),
                LocalDateTime.of(2026, 8, 20, 18, 0), MedioPago.EFECTIVO).isEmpty());
        assertTrue(promociones.mejorPara(List.of(entrada(5000)),
                LocalDateTime.of(2026, 8, 20, 23, 30), MedioPago.EFECTIVO).isPresent());
    }

    @Test
    void unaPromocionDesactivadaDejaDeCorrer() {
        Promocion promo = promociones.crearPorcentaje("30 off", 30, DESDE, HASTA,
                Set.of(), null, null, Set.of());
        promociones.desactivar(promo.getId());

        assertTrue(promociones.mejorPara(List.of(entrada(5000)), JUEVES, MedioPago.EFECTIVO).isEmpty());
    }

    // ---------- validaciones del alta ----------

    /** Un 2x2 no descuenta nada y un 2x3 cobraría de más. */
    @Test
    void rechazaUnNxMQueNoDescuenta() {
        assertThrows(IllegalArgumentException.class, () -> promociones.crearNxM("2x2", 2, 2,
                DESDE, HASTA, Set.of(), null, null, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> promociones.crearNxM("2x3", 2, 3,
                DESDE, HASTA, Set.of(), null, null, Set.of()));
    }

    @Test
    void rechazaUnPorcentajeFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> promociones.crearPorcentaje("gratis", 100,
                DESDE, HASTA, Set.of(), null, null, Set.of()));
    }

    @Test
    void rechazaUnaVigenciaAlReves() {
        assertThrows(IllegalArgumentException.class, () -> promociones.crearPorcentaje("rara", 10,
                HASTA, DESDE, Set.of(), null, null, Set.of()));
    }

    @Test
    void rechazaDosPromocionesConElMismoNombre() {
        promociones.crearPorcentaje("30 off", 30, DESDE, HASTA, Set.of(), null, null, Set.of());

        assertThrows(IllegalArgumentException.class, () -> promociones.crearMontoFijo("30 off", Dinero.de(500),
                DESDE, HASTA, Set.of(), null, null, Set.of()));
    }
}
