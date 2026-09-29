package ar.uade.cine.service.informes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.promociones.CondicionesPromocion;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.candy.GestorProductos;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.informes.GestorCaja;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.promociones.GestorPromociones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.CalculadoraPrecio;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.Ocupacion;

class GestorInformesTest extends PruebaDeIntegracion {

    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorPagos pagos;
    @Autowired
    private GestorPromociones promociones;
    @Autowired
    private GestorProductos productos;
    @Autowired
    private GestorCandy candy;
    @Autowired
    private GestorCaja caja;
    @Autowired
    private GestorInformes informes;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorClientes clientes;

    @BeforeEach
    void prepararEscenario() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
        clientes.registrar("Andrei", "andrei@uade.edu.ar");
    }

    @Test
    void elBorderoTitulaConLaPeliculaLaSalaYElHorarioDeLaFuncion() {
        Bordero bordero = informes.borderoDe(1);

        assertEquals("Matrix", bordero.pelicula());
        assertEquals("Sala 1", bordero.sala());
        assertEquals(LocalDateTime.of(2026, 8, 20, 20, 0), bordero.funcion());
    }

    @Test
    void elBorderoNoCuentaLasReservasSinPagar() {
        Reserva cobrada = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL), null);
        reservas.reservar(1, 1, butacas("A2", TipoTarifa.GENERAL), null);
        pagos.cobrar(cobrada.getId(), MedioPago.EFECTIVO, "");

        Bordero bordero = informes.borderoDe(1);

        assertEquals(1, bordero.espectadores());
        assertEquals(Dinero.de(5000.0), bordero.recaudacionNeta());
    }

    @Test
    void elBorderoNoCuentaLasEntradasDeOtraFuncion() {
        Reserva primera = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL), null);
        Reserva otraFuncion = reservas.reservar(2, 1, butacas("A1", TipoTarifa.GENERAL), null);
        pagos.cobrar(primera.getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(otraFuncion.getId(), MedioPago.EFECTIVO, "");

        assertEquals(1, informes.borderoDe(1).espectadores());
        assertEquals(1, informes.borderoDe(2).espectadores());
    }

    @Test
    void elBorderoDesglosaCuantasEntradasSalieronACadaTarifa() {
        Map<String, TipoTarifa> pedido = new LinkedHashMap<>();
        pedido.put("A1", TipoTarifa.GENERAL);
        pedido.put("A2", TipoTarifa.GENERAL);
        pedido.put("A3", TipoTarifa.JUBILADO);
        Reserva reserva = reservas.reservar(1, 1, pedido, null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Bordero bordero = informes.borderoDe(1);

        assertEquals(3, bordero.espectadores());
        assertEquals(2, bordero.porTarifa().get(TipoTarifa.GENERAL).cantidad());
        assertEquals(Dinero.de(10000.0), bordero.porTarifa().get(TipoTarifa.GENERAL).total());
        assertEquals(1, bordero.porTarifa().get(TipoTarifa.JUBILADO).cantidad());
        assertEquals(Dinero.de(2500.0), bordero.porTarifa().get(TipoTarifa.JUBILADO).total());
        assertEquals(Dinero.de(12500.0), bordero.recaudacionBruta());
    }

    @Test
    void elBorderoSeparaElBrutoDelDescuentoYDelNeto() {
        promociones.crearPorcentaje("50 off", 50.0,
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()));
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Bordero bordero = informes.borderoDe(1);

        assertEquals(Dinero.de(5000.0), bordero.recaudacionBruta());
        assertEquals(Dinero.de(2500.0), bordero.descuentos());
        assertEquals(Dinero.de(2500.0), bordero.recaudacionNeta());
    }

    @Test
    void elBorderoDeUnaFuncionSinVentasDaEnCero() {
        Bordero bordero = informes.borderoDe(1);

        assertEquals(0, bordero.espectadores());
        assertEquals(Dinero.de(0), bordero.recaudacionBruta());
        assertEquals(Dinero.de(0), bordero.recaudacionNeta());
        assertTrue(bordero.porTarifa().isEmpty());
    }

    @Test
    void noHayBorderoDeUnaFuncionQueNoExiste() {
        assertThrows(Rechazo.class, () -> informes.borderoDe(99));
    }

    @Test
    void elInformeSumaLasEntradasYElCandyDeLaFuncion() {
        Producto pochoclos = productos.agregar("Pochoclos",
                TipoProducto.POCHOCLOS, Dinero.de(3000));
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        candy.venderParaReserva(reserva.getId(), Map.of(pochoclos.getId(), 2),
                MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(1);

        assertEquals(Dinero.de(5000.0), informe.bordero().recaudacionNeta());
        assertEquals(1, informe.comprasCandy());
        assertEquals(Dinero.de(6000.0), informe.candy());
        assertEquals(Dinero.de(11000.0), informe.total());
    }

    @Test
    void elCandyDeMostradorNoEntraEnElInformeDeNingunaFuncion() {
        Producto pochoclos = productos.agregar("Pochoclos",
                TipoProducto.POCHOCLOS, Dinero.de(3000));
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        candy.vender(null, Map.of(pochoclos.getId(), 1), MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(1);

        assertEquals(0, informe.comprasCandy());
        assertEquals(Dinero.de(0), informe.candy());
        assertEquals(Dinero.de(5000.0), informe.total());
        assertEquals(Dinero.de(3000.0), caja.arqueoCandyDe(reloj.hoy()).total());
    }

    @Test
    void elCandyDeOtraFuncionNoEntraEnEsteInforme() {
        Producto pochoclos = productos.agregar("Pochoclos",
                TipoProducto.POCHOCLOS, Dinero.de(3000));
        Reserva deLaOtra = reservas.reservar(2, 1, butacas("A1", TipoTarifa.GENERAL), null);
        pagos.cobrar(deLaOtra.getId(), MedioPago.EFECTIVO, "");
        candy.venderParaReserva(deLaOtra.getId(), Map.of(pochoclos.getId(), 1),
                MedioPago.EFECTIVO, "");

        assertEquals(Dinero.de(0), informes.informeDe(1).candy());
        assertEquals(Dinero.de(3000.0), informes.informeDe(2).candy());
    }

    @Test
    void unaFuncionSinCandyRecaudaLoMismoQueSuBordero() {
        Reserva reserva = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        InformeFuncion informe = informes.informeDe(1);

        assertEquals(informe.bordero().recaudacionNeta(), informe.total());
    }

    @Test
    void sinFechasLaDeclaracionEsLaSemanaCinematograficaAnteriorDeJuevesAMiercoles() {
        reloj.mover(LocalDateTime.of(2026, 8, 28, 10, 0));

        DeclaracionJurada declaracion = informes.declaracionJurada(null, null);

        assertEquals(LocalDate.of(2026, 8, 20), declaracion.desde());
        assertEquals(LocalDate.of(2026, 8, 26), declaracion.hasta());
    }

    @Test
    void unJuevesLaSemanaAnteriorEsLaQueTerminoAyer() {
        reloj.mover(LocalDateTime.of(2026, 8, 27, 10, 0));

        DeclaracionJurada declaracion = informes.declaracionJurada(null, null);

        assertEquals(LocalDate.of(2026, 8, 20), declaracion.desde());
        assertEquals(LocalDate.of(2026, 8, 26), declaracion.hasta());
    }

    @Test
    void laDeclaracionSoloTraeLasFuncionesDelPeriodoConEntradasCobradas() {
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 27, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
        Reserva cobrada = reservas.reservar(1, 1, butacas("A1", TipoTarifa.GENERAL), null);
        reservas.reservar(2, 1, butacas("A1", TipoTarifa.GENERAL), null);
        Reserva fueraDelPeriodo = reservas.reservar(3, 1, butacas("A1", TipoTarifa.GENERAL), null);
        pagos.cobrar(cobrada.getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(fueraDelPeriodo.getId(), MedioPago.EFECTIVO, "");

        DeclaracionJurada declaracion = informes.declaracionJurada(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 26));

        assertEquals(1, declaracion.funciones().size());
        assertEquals(1, declaracion.funciones().get(0).bordero().funcionId());
        assertEquals(1, declaracion.total().espectadores());
    }

    @Test
    void cadaFilaDeLaDeclaracionEsElBorderoDeSuFuncion() {
        promociones.crearPorcentaje("50 off", 50.0,
                new CondicionesPromocion(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Set.of(), null, null, Set.of()));
        Map<String, TipoTarifa> pedido = new LinkedHashMap<>();
        pedido.put("A1", TipoTarifa.GENERAL);
        pedido.put("A2", TipoTarifa.JUBILADO);
        Reserva reserva = reservas.reservar(1, 1, pedido, null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        DeclaracionJurada.FilaFuncion fila = informes.declaracionJurada(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 20)).funciones().get(0);

        assertEquals(informes.borderoDe(1), fila.bordero());
        assertEquals(Version.DOBLADA, fila.version());
        assertEquals(Proyeccion.DOS_D, fila.proyeccion());
        assertEquals(Clasificacion.MAS_13, fila.clasificacion());
    }

    @Test
    void laDeclaracionTotalizaPorPeliculaYEnGeneral() {
        cartelera.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.ATP);
        funciones.programar(2, 1, LocalDateTime.of(2026, 8, 22, 18, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4000));
        Map<String, TipoTarifa> pedido = new LinkedHashMap<>();
        pedido.put("A1", TipoTarifa.GENERAL);
        pedido.put("A2", TipoTarifa.MENOR);
        pagos.cobrar(reservas.reservar(1, 1, pedido, null).getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(reservas.reservar(2, 1, butacas("A1", TipoTarifa.GENERAL), null).getId(), MedioPago.EFECTIVO, "");
        pagos.cobrar(reservas.reservar(3, 1, butacas("A1", TipoTarifa.GENERAL), null).getId(), MedioPago.EFECTIVO, "");

        DeclaracionJurada declaracion = informes.declaracionJurada(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 26));

        assertEquals(List.of("Dune", "Matrix"),
                declaracion.peliculas().stream().map(DeclaracionJurada.TotalPelicula::titulo).toList());
        DeclaracionJurada.Totales matrix = declaracion.peliculas().get(1).totales();
        assertEquals(2, matrix.funciones());
        assertEquals(3, matrix.espectadores());
        assertEquals(2, matrix.entradas(TipoTarifa.GENERAL));
        assertEquals(1, matrix.entradas(TipoTarifa.MENOR));
        assertEquals(Dinero.de(13000.0), matrix.recaudacionNeta());
        DeclaracionJurada.Totales total = declaracion.total();
        assertEquals(3, total.funciones());
        assertEquals(4, total.espectadores());
        assertEquals(Dinero.de(17000.0), total.recaudacionBruta());
    }

    @Test
    void unPeriodoSinVentasDaUnaDeclaracionEnCero() {
        DeclaracionJurada declaracion = informes.declaracionJurada(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 26));

        assertTrue(declaracion.funciones().isEmpty());
        assertTrue(declaracion.peliculas().isEmpty());
        assertEquals(0, declaracion.total().espectadores());
        assertEquals(Dinero.CERO, declaracion.total().recaudacionNeta());
    }

    @Test
    void laDeclaracionRechazaUnPeriodoAlReves() {
        Rechazo error = assertThrows(Rechazo.class,
                () -> informes.declaracionJurada(LocalDate.of(2026, 8, 26), LocalDate.of(2026, 8, 20)));

        assertEquals("El período tiene que empezar antes de terminar", error.getMessage());
    }

    @Test
    void laDeclaracionRechazaUnPeriodoDeMasDeUnMes() {
        assertThrows(Rechazo.class,
                () -> informes.declaracionJurada(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)));
        assertEquals(LocalDate.of(2026, 8, 31), informes.declaracionJurada(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)).hasta());
    }

    @Test
    void laDeclaracionPideLasDosFechasONinguna() {
        assertThrows(Rechazo.class,
                () -> informes.declaracionJurada(LocalDate.of(2026, 8, 20), null));
    }

    private static Map<String, TipoTarifa> butacas(String codigo, TipoTarifa tarifa) {
        return Map.of(codigo, tarifa);
    }
}
