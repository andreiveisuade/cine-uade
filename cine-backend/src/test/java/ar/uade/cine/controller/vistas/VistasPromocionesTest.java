package ar.uade.cine.controller.vistas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.dto.promociones.PromocionVistaDTO;
import ar.uade.cine.repository.memoria.PromocionDAOMemoria;
import ar.uade.cine.service.promociones.GestorPromociones;
import ar.uade.cine.model.dinero.Dinero;

/**
 * Esta vista es el único lugar del sistema que pregunta de qué tipo es una promoción, y
 * con un motivo: cada tipo expresa su beneficio con un campo distinto y el JSON tiene que
 * mostrarlo. El cálculo del descuento sigue siendo polimórfico y no pasa por acá.
 *
 * <p>Estas pruebas fijan las dos mitades de esa decisión: que cada tipo mande su campo, y
 * que <strong>no</strong> mande los de los otros. Si mañana aparece un tipo nuevo, este
 * archivo es el que avisa que hay que tocarlo.
 */
class VistasPromocionesTest {

    private GestorPromociones promociones;
    private VistasPromociones vistas;

    @BeforeEach
    void prepararEscenario() {
        promociones = new GestorPromociones(new PromocionDAOMemoria());
        vistas = new VistasPromociones();
    }

    @Test
    void elPorcentajeMandaSuPorcentajeYNadaMas() {
        Promocion promocion = promociones.crearPorcentaje("Martes 30%", 30,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(DayOfWeek.TUESDAY), null, null, Set.of());

        PromocionVistaDTO vista = vistas.promocion(promocion);

        assertEquals("PORCENTAJE", vista.tipo());
        assertEquals(30.0, vista.porcentaje());
        assertNull(vista.monto(), "el monto es de otro tipo de promoción");
        assertNull(vista.lleva());
        assertNull(vista.paga());
    }

    @Test
    void elMontoFijoMandaSuMontoYNadaMas() {
        Promocion promocion = promociones.crearMontoFijo("$2000 off", Dinero.de(2000),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());

        PromocionVistaDTO vista = vistas.promocion(promocion);

        assertEquals("MONTO_FIJO", vista.tipo());
        assertEquals(2000.0, vista.monto());
        assertNull(vista.porcentaje());
    }

    @Test
    void elNxMMandaCuantoSeLlevaYCuantoSePaga() {
        Promocion promocion = promociones.crearNxM("2x1", 2, 1,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());

        PromocionVistaDTO vista = vistas.promocion(promocion);

        assertEquals("NXM", vista.tipo());
        assertEquals(2, vista.lleva());
        assertEquals(1, vista.paga());
        assertNull(vista.porcentaje());
        assertNull(vista.monto());
    }

    /**
     * Sin días significa todos los días y sin medios significa cualquiera: es como lo lee
     * el gestor. Viajan como listas vacías y no como null, porque el front las recorre.
     */
    @Test
    void lasCondicionesVaciasViajanComoListasVacias() {
        Promocion promocion = promociones.crearPorcentaje("Siempre 10%", 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());

        PromocionVistaDTO vista = vistas.promocion(promocion);

        assertEquals(List.of(), vista.diasSemana());
        assertEquals(List.of(), vista.mediosPago());
        assertNull(vista.horaDesde(), "sin franja horaria no hay hora que mostrar");
        assertNull(vista.horaHasta());
    }

    @Test
    void lasCondicionesCargadasViajanCompletas() {
        Promocion promocion = promociones.crearPorcentaje("Trasnoche", 25,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                Set.of(DayOfWeek.FRIDAY), LocalTime.of(22, 0), LocalTime.of(23, 59),
                Set.of(MedioPago.EFECTIVO));

        PromocionVistaDTO vista = vistas.promocion(promocion);

        assertEquals("2026-03-01", vista.vigenciaDesde());
        assertEquals("2026-03-31", vista.vigenciaHasta());
        assertEquals(List.of("FRIDAY"), vista.diasSemana());
        assertEquals(List.of("EFECTIVO"), vista.mediosPago());
        assertEquals("22:00", vista.horaDesde());
        assertEquals("23:59", vista.horaHasta());
    }

    /** Desactivada sigue viajando: el ABM la lista para poder volver a prenderla. */
    @Test
    void laPromocionDesactivadaViajaMarcadaComoInactiva() {
        Promocion promocion = promociones.crearPorcentaje("Martes 30%", 30,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of());
        assertTrue(vistas.promocion(promocion).activa());

        promociones.desactivar(promocion.getId());

        assertFalse(vistas.promocion(promociones.buscar(promocion.getId()).orElseThrow()).activa());
    }
}
