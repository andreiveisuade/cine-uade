package ar.uade.cine.service.promociones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.promociones.CondicionesPromocion;
import ar.uade.cine.model.promociones.ParametrosPromocion;
import ar.uade.cine.model.promociones.Promocion;
import ar.uade.cine.model.promociones.TipoPromocion;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.promociones.PoliticaPromociones.Descuento;

class GestorPromocionesTest extends PruebaDeIntegracion {

    private static final LocalDateTime JUEVES = LocalDateTime.of(2026, 8, 20, 20, 0);
    private static final LocalDateTime MIERCOLES = LocalDateTime.of(2026, 8, 19, 20, 0);
    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);
    private static final CondicionesPromocion AGOSTO = condiciones(Set.of(), null, Set.of());

    private static final Asiento A1 = new Asiento(null, 1, 1, TipoAsiento.ESTANDAR);

    @Autowired
    private GestorPromociones promociones;

    private static CondicionesPromocion condiciones(Set<DayOfWeek> dias, LocalTime desdeLas,
                                                    Set<MedioPago> medios) {
        return new CondicionesPromocion(DESDE, HASTA, dias, desdeLas, null, medios);
    }

    private static Entrada entrada(double precio) {
        return entrada(precio, TipoTarifa.GENERAL);
    }

    private static Entrada entrada(double precio, TipoTarifa tarifa) {
        return new Entrada(A1, tarifa, Dinero.de(precio));
    }

    private Promocion porcentaje(String nombre, double porcentaje) {
        return promociones.crear(TipoPromocion.PORCENTAJE, nombre, ParametrosPromocion.dePorcentaje(porcentaje),
                AGOSTO);
    }

    private Promocion dosPorUno(String nombre) {
        return promociones.crear(TipoPromocion.NXM, nombre, ParametrosPromocion.deNxM(2, 1), AGOSTO);
    }

    private boolean corre(List<Entrada> entradas, LocalDateTime inicioFuncion, MedioPago medio) {
        return promociones.calcularPara(entradas, inicioFuncion, medio).promocionId() != null;
    }

    @Test
    void elPorcentajeDescuentaSobreElSubtotal() {
        Promocion promo = porcentaje("30 off", 30);

        assertEquals(Dinero.de(3000), promo.calcularDescuento(List.of(entrada(5000), entrada(5000))));
    }

    @Test
    void elMontoFijoNuncaDescuentaMasQueElTotal() {
        Promocion promo = promociones.crear(TipoPromocion.MONTO_FIJO, "2000 off",
                ParametrosPromocion.deMonto(2000.0), AGOSTO);

        assertEquals(Dinero.de(1500), promo.calcularDescuento(List.of(entrada(1500))));
    }

    @Test
    void elDosPorUnoRegalaLaMasBarata() {
        Promocion promo = dosPorUno("2x1");

        assertEquals(Dinero.de(4000), promo.calcularDescuento(List.of(entrada(6000), entrada(4000))));
    }

    @Test
    void elDosPorUnoSoloCuentaGruposCompletos() {
        Promocion promo = dosPorUno("2x1");

        assertEquals(Dinero.de(5000), promo.calcularDescuento(
                List.of(entrada(5000), entrada(5000), entrada(5000))));
    }

    @Test
    void unaSolaEntradaNoActivaElDosPorUno() {
        Promocion promo = dosPorUno("2x1");

        assertEquals(Dinero.de(0), promo.calcularDescuento(List.of(entrada(5000))));
    }

    @Test
    void ganaLaQueMasDescuenta() {
        porcentaje("10 off", 10);
        Promocion dosPorUno = dosPorUno("2x1");

        List<Entrada> dos = List.of(entrada(5000), entrada(5000));
        Descuento descuento = promociones.calcularPara(dos, JUEVES, MedioPago.EFECTIVO);

        assertEquals(dosPorUno.getId(), descuento.promocionId(), "el 2x1 saca 5000 y el 10% solo 1000");
        assertEquals(Dinero.de(5000), descuento.monto());
    }

    @Test
    void enUnEmpateGanaLaDeMenorId() {
        Promocion primera = porcentaje("primera", 20);
        porcentaje("segunda", 20);

        Descuento descuento = promociones.calcularPara(
                List.of(entrada(5000)), JUEVES, MedioPago.EFECTIVO);

        assertEquals(primera.getId(), descuento.promocionId());
    }

    @Test
    void laTarifaReducidaNoParticipaDelDescuento() {
        porcentaje("50 off", 50);

        List<Entrada> mixta = List.of(entrada(5000), entrada(2500, TipoTarifa.JUBILADO));

        assertEquals(Dinero.de(2500), promociones.calcularPara(mixta, JUEVES, MedioPago.EFECTIVO).monto(),
                "el 50% corre solo sobre los 5000 de la general");
    }

    @Test
    void sinEntradasGeneralesNoAplicaNinguna() {
        porcentaje("50 off", 50);

        assertFalse(corre(List.of(entrada(2500, TipoTarifa.JUBILADO)),
                JUEVES, MedioPago.EFECTIVO));
    }

    @Test
    void laDelMiercolesNoCorreUnJueves() {
        promociones.crear(TipoPromocion.NXM, "Miércoles 2x1", ParametrosPromocion.deNxM(2, 1),
                condiciones(Set.of(DayOfWeek.WEDNESDAY), null, Set.of()));

        List<Entrada> dos = List.of(entrada(5000), entrada(5000));

        assertTrue(corre(dos, MIERCOLES, MedioPago.EFECTIVO));
        assertFalse(corre(dos, JUEVES, MedioPago.EFECTIVO));
    }

    @Test
    void laDelBancoSoloCorreConEseMedioDePago() {
        promociones.crear(TipoPromocion.MONTO_FIJO, "Banco", ParametrosPromocion.deMonto(1000.0),
                condiciones(Set.of(), null, Set.of(MedioPago.CREDITO)));

        List<Entrada> una = List.of(entrada(5000));

        assertTrue(corre(una, JUEVES, MedioPago.CREDITO));
        assertFalse(corre(una, JUEVES, MedioPago.EFECTIVO));
    }

    @Test
    void fueraDeVigenciaNoCorre() {
        promociones.crear(TipoPromocion.PORCENTAJE, "Septiembre", ParametrosPromocion.dePorcentaje(30.0),
                new CondicionesPromocion(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), Set.of(), null, null,
                        Set.of()));

        assertFalse(corre(List.of(entrada(5000)), JUEVES, MedioPago.EFECTIVO));
    }

    @Test
    void laTrasnocheNoCorreEnLaFuncionDeLaTarde() {
        promociones.crear(TipoPromocion.PORCENTAJE, "Trasnoche", ParametrosPromocion.dePorcentaje(40.0),
                condiciones(Set.of(), LocalTime.of(23, 0), Set.of()));

        assertFalse(corre(List.of(entrada(5000)),
                LocalDateTime.of(2026, 8, 20, 18, 0), MedioPago.EFECTIVO));
        assertTrue(corre(List.of(entrada(5000)),
                LocalDateTime.of(2026, 8, 20, 23, 30), MedioPago.EFECTIVO));
    }

    @Test
    void unaPromocionDesactivadaDejaDeCorrer() {
        Promocion promo = porcentaje("30 off", 30);
        promociones.desactivar(promo.getId());

        assertFalse(corre(List.of(entrada(5000)), JUEVES, MedioPago.EFECTIVO));
    }

    @Test
    void rechazaUnNxMQueNoDescuenta() {
        assertThrows(Rechazo.class, () -> promociones.crear(TipoPromocion.NXM, "2x2",
                ParametrosPromocion.deNxM(2, 2), AGOSTO));
        assertThrows(Rechazo.class, () -> promociones.crear(TipoPromocion.NXM, "2x3",
                ParametrosPromocion.deNxM(2, 3), AGOSTO));
    }

    @Test
    void rechazaUnPorcentajeFueraDeRango() {
        assertThrows(Rechazo.class, () -> porcentaje("gratis", 100));
    }

    @Test
    void rechazaUnaVigenciaAlReves() {
        assertThrows(Rechazo.class, () -> promociones.crear(TipoPromocion.PORCENTAJE, "rara",
                ParametrosPromocion.dePorcentaje(10.0),
                new CondicionesPromocion(HASTA, DESDE, Set.of(), null, null, Set.of())));
    }

    // El día de hoy lo pone el gestor desde el reloj: la misma promoción de agosto ya no se carga en septiembre.
    @Test
    void unaPromocionYaVencidaSegunElRelojNoSeCarga() {
        reloj.mover(LocalDateTime.of(2026, 9, 1, 10, 0));

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> porcentaje("Agosto", 10));

        assertEquals("La vigencia ya terminó: el fin tiene que ser hoy o después", error.getMessage());
    }

    @Test
    void lasPromocionesQueNoExistenSon404() {
        RecursoNoEncontrado error = assertThrows(RecursoNoEncontrado.class, () -> promociones.activar(99));

        assertEquals("No existe la promoción 99", error.getMessage());
    }

    @Test
    void rechazaDosPromocionesConElMismoNombre() {
        porcentaje("30 off", 30);

        assertThrows(Rechazo.class, () -> promociones.crear(TipoPromocion.MONTO_FIJO, "30 off",
                ParametrosPromocion.deMonto(500.0), AGOSTO));
    }

    @Test
    void unNombreQueSoloCambiaEnLosEspaciosTambienEstaRepetido() {
        porcentaje("30 off ", 30);

        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class, () -> promociones.crear(
                TipoPromocion.MONTO_FIJO, "30 off", ParametrosPromocion.deMonto(500.0), AGOSTO));
        assertEquals("Ya existe una promoción con ese nombre", error.getMessage());
    }
}
