package ar.uade.cine.service.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.repository.AsientoRepository;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.promociones.GestorPromociones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.model.dinero.Dinero;

/** R4 (butaca libre), R6 (cancelar libera las butacas) y R13 (no se cancela lo cobrado). */
class GestorReservasTest extends PruebaDeIntegracion {

    private static final Path TICKETS = Path.of("target/comprobantes/tickets");

    @Autowired
    private GestorReservas reservas;
    @Autowired
    private Ocupacion ocupacion;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorPagos pagos;
    @Autowired
    private GestorClientes clientes;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private FuncionRepository funcionRepository;
    @Autowired
    private AsientoRepository asientoRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private LocalDateTime ahora;

    /** Sala de 2 filas x 5 butacas (A1..A5, B1..B5), una función a $5000, un cliente. */
    @BeforeEach
    void prepararEscenario() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        clientes.registrar("Andrei", "andrei@uade.edu.ar");

        ahora = LocalDateTime.of(2026, 8, 14, 10, 0);
        reloj.mover(ahora);
    }

    @Test
    void reservarOcupaSoloLasButacasElegidas() {
        reservas.reservar(1, 1, generales("A1", "A2"));

        assertEquals(8, ocupacion.lugaresLibres(1));
        assertTrue(ocupacion.asientosLibres(1).stream().noneMatch(a -> a.getCodigo().equals("A1")));
        assertTrue(ocupacion.asientosLibres(1).stream().anyMatch(a -> a.getCodigo().equals("A3")));
    }

    @Test
    void rechazaButacaYaOcupada() {
        reservas.reservar(1, 1, generales("B3"));
        assertThrows(IllegalArgumentException.class, () -> reservas.reservar(1, 1, generales("B3")));
    }

    @Test
    void rechazaButacaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> reservas.reservar(1, 1, generales("Z9")));
    }

    @Test
    void reservarDaDeAltaAlClienteQueNoExistia() {
        Reserva reserva = reservas.reservar(1, "Nueva", "nueva@uade.edu.ar", generales("A1"), null);

        assertEquals(clientes.buscarPorEmail("nueva@uade.edu.ar").orElseThrow().getId(), reserva.getClienteId());
    }

    @Test
    void unaReservaRechazadaNoDejaAlClienteDadoDeAlta() {
        assertThrows(IllegalArgumentException.class,
                () -> reservas.reservar(1, "Nueva", "nueva@uade.edu.ar", generales("Z9"), null));

        assertTrue(clientes.buscarPorEmail("nueva@uade.edu.ar").isEmpty());
    }

    /** Las butacas viajan como mapa de código a tarifa: pedir A1 dos veces no se puede expresar. */
    @Test
    void laMismaButacaDosVecesEsUnaSolaEntrada() {
        assertEquals(1, reservas.reservar(1, 1, generales("A1", "A1")).getCantidadEntradas());
    }

    @Test
    void laTarifaReducidaAbarataSoloSuButaca() {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        butacas.put("A1", TipoTarifa.GENERAL);
        butacas.put("A2", TipoTarifa.JUBILADO);

        Reserva reserva = reservas.reservar(1, 1, butacas);
        Dinero general = precioDe(reserva, "A1");
        Dinero jubilado = precioDe(reserva, "A2");

        assertEquals(general.por(TipoTarifa.JUBILADO.getMultiplicadorPrecio()), jubilado);
        assertEquals(general.mas(jubilado), reserva.getTotal());
    }

    @Test
    void cadaEntradaRecuerdaConQueTarifaSeVendio() {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        butacas.put("A1", TipoTarifa.MENOR);
        butacas.put("A2", TipoTarifa.ESTUDIANTE);

        Reserva reserva = reservas.reservar(1, 1, butacas);

        assertEquals(TipoTarifa.MENOR, tarifaDe(reserva, "A1"));
        assertEquals(TipoTarifa.ESTUDIANTE, tarifaDe(reserva, "A2"));
    }

    /** Sin login no se puede validar quién es: la tarifa se acredita en la puerta. */
    @Test
    void soloLaTarifaGeneralNoPideAcreditacion() {
        assertFalse(TipoTarifa.GENERAL.requiereAcreditacion());
        assertTrue(TipoTarifa.JUBILADO.requiereAcreditacion());
        assertTrue(TipoTarifa.MENOR.requiereAcreditacion());
        assertTrue(TipoTarifa.ESTUDIANTE.requiereAcreditacion());
    }

    @Test
    void cancelarLiberaLasButacas() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2", "A3"));
        reservas.cancelar(reserva.getId());

        assertEquals(10, ocupacion.lugaresLibres(1));
        assertTrue(ocupacion.asientosLibres(1).stream().anyMatch(a -> a.getCodigo().equals("A1")));
    }

    /** R13: si se cancelara, el pago seguiría contando en el arqueo del día. */
    @Test
    void noSePuedeCancelarUnaReservaYaCobrada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertThrows(IllegalArgumentException.class, () -> reservas.cancelar(reserva.getId()));
        assertEquals(9, ocupacion.lugaresLibres(1), "la butaca cobrada sigue ocupada");
    }

    @Test
    void noSeCancelaDosVecesLaMismaReserva() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        reservas.cancelar(reserva.getId());

        assertThrows(IllegalArgumentException.class, () -> reservas.cancelar(reserva.getId()));
    }

    @Test
    void unaButacaFueraDeServicioNoSePuedeReservar() {
        salas.marcarFueraDeServicio(1, "A3");

        assertEquals(9, ocupacion.lugaresLibres(1));
        assertTrue(ocupacion.asientosLibres(1).stream().noneMatch(a -> a.getCodigo().equals("A3")));
        assertThrows(IllegalArgumentException.class, () -> reservas.reservar(1, 1, generales("A3")));
    }

    @Test
    void reponerLaButacaLaVuelveAHabilitar() {
        salas.marcarFueraDeServicio(1, "A3");
        salas.reponer(1, "A3");

        assertEquals(10, ocupacion.lugaresLibres(1));
        assertEquals(1, reservas.reservar(1, 1, generales("A3")).getCantidadEntradas());
    }

    @Test
    void emiteElTicketConLasButacas() throws IOException {
        Reserva reserva = reservas.reservar(1, 1, generales("B4", "B5"));

        Path ticket = TICKETS.resolve("ticket-" + reserva.getId() + ".txt");
        assertTrue(Files.exists(ticket), "no se generó el ticket");

        String contenido = Files.readString(ticket);
        assertTrue(contenido.contains("Matrix"), "el ticket no menciona la película");
        assertTrue(contenido.contains("Sala 1"), "el ticket no menciona la sala");
        assertTrue(contenido.contains("Andrei"), "el ticket no menciona al cliente");
        assertTrue(contenido.contains("Butaca B4"), "el ticket no lista la butaca B4");
        assertTrue(contenido.contains("Butaca B5"), "el ticket no lista la butaca B5");
        assertTrue(contenido.contains("10000"), "el total deberia ser 2 x 5000");
    }

    @Test
    void elPrecioDependeDelTipoDeSalaYDeButaca() {
        // Sala 2 es IMAX (x1.6) y su butaca A1 es VIP (x1.5); base 5000 => 12000
        salas.agregar("Sala 2", TipoSala.IMAX, List.of(4), Map.of("A1", TipoAsiento.VIP));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5000));

        Reserva vip = reservas.reservar(2, 1, generales("A1"));
        assertEquals(Dinero.de(12000.0), vip.getTotal());

        Reserva estandar = reservas.reservar(2, 1, generales("A2"));
        assertEquals(Dinero.de(8000.0), estandar.getTotal());
    }

    @Test
    void elRecargoPremiumSeAplicaUnaSolaVez() {
        salas.agregar("Sala VIP", TipoSala.IMAX, List.of(2), Map.of("A1", TipoAsiento.VIP));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 22, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));

        // 5000 x 1.6 (IMAX) x 1.5 (butaca VIP), y nada más
        assertEquals(Dinero.de(12000.0), reservas.reservar(2, 1, generales("A1")).getTotal());
    }

    /** Sin redondeo, 5250.50 x 1.3 da 6825.650000000001 y eso llega al ticket. */
    @Test
    void elPrecioSeRedondeaADosDecimales() {
        salas.agregar("Sala 3D", TipoSala.TRES_D, List.of(3));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 23, 20, 0),
                Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5250.50));

        assertEquals(Dinero.de(6825.65), reservas.reservar(2, 1, generales("A1")).getTotal());
    }

    @Test
    void guardaCuandoSeHizoLaReserva() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));

        assertEquals(reloj.hoy(), reserva.getCreadaEn().toLocalDate());
    }

    @Test
    void noSePuedeProgramar3DEnUnaSalaQueNoLoSoporta() {
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(4));
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 24, 20, 0),
                        Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5000)));
    }

    /** El código de butaca no está en la tabla entrada, sale del asiento por la relación. */
    @Test
    void loGuardadoSeReleeConSusButacasYSuFecha() {
        reservas.reservar(1, 1, generales("A1", "B2"));

        Reserva leida = reservaRepository.findById(1).orElseThrow();

        assertEquals(2, leida.getCantidadEntradas());
        assertEquals("A1", leida.getEntradas().get(0).codigoAsiento());
        assertEquals(reloj.hoy(), leida.getCreadaEn().toLocalDate());
    }


    /** Corre la fecha de creación hacia atrás en vez de esperar el vencimiento. */
    private void envejecer(int reservaId, int minutos) {
        jdbc.update("UPDATE reserva SET creada_en = ? WHERE id = ?",
                reservaRepository.findById(reservaId).orElseThrow()
                        .getCreadaEn().minusMinutes(minutos),
                reservaId);
    }

    @Test
    void unaReservaSinPagarVencidaLiberaSusButacas() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2"));
        assertEquals(8, ocupacion.lugaresLibres(1));

        envejecer(reserva.getId(), Reserva.MINUTOS_PARA_PAGAR + 1);

        assertEquals(10, ocupacion.lugaresLibres(1), "las butacas vuelven a la venta");
        assertEquals(EstadoReserva.EXPIRADA, reservas.buscar(reserva.getId()).orElseThrow().getEstado());
    }

    /** Se expira al consultar, no con un proceso de fondo. */
    @Test
    void laReservaVencidaSeCierraReciénCuandoAlguienConsulta() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        envejecer(reserva.getId(), Reserva.MINUTOS_PARA_PAGAR + 1);

        assertEquals(EstadoReserva.RESERVADA, reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
        ocupacion.lugaresLibres(1);
        assertEquals(EstadoReserva.EXPIRADA, reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
    }

    @Test
    void unaReservaPagadaNoVence() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        envejecer(reserva.getId(), Reserva.MINUTOS_PARA_PAGAR + 1);

        assertEquals(9, ocupacion.lugaresLibres(1), "la butaca cobrada sigue ocupada");
    }

    /** R17: si venció, sus butacas ya se pueden estar vendiendo a otro. */
    @Test
    void noSeCobraUnaReservaVencida() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        envejecer(reserva.getId(), Reserva.MINUTOS_PARA_PAGAR + 1);

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    @Test
    void elCodigoNoEsElIdYNoSeRepite() {
        Reserva primera = reservas.reservar(1, 1, generales("A1"));
        Reserva segunda = reservas.reservar(1, 1, generales("A2"));

        assertNotEquals(primera.getCodigo(), segunda.getCodigo());
        assertNotEquals(String.valueOf(primera.getId()), primera.getCodigo());
        assertEquals(8, primera.getCodigo().length());
    }

    /** R18: en la puerta entra una reserva pagada, y una sola vez. */
    @Test
    void seIngresaUnaSolaVezYSoloSiEstaPagada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"));
        assertThrows(IllegalArgumentException.class,
                () -> reservas.registrarIngreso(reserva.getCodigo()), "sin pagar no entra");

        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        assertNotNull(reservas.registrarIngreso(reserva.getCodigo()).getIngresadaEn());

        assertThrows(IllegalArgumentException.class,
                () -> reservas.registrarIngreso(reserva.getCodigo()), "no entra dos veces");
    }

    @Test
    void unCodigoInventadoNoAbreLaPuerta() {
        assertThrows(IllegalArgumentException.class, () -> reservas.registrarIngreso("XXXXXXXX"));
    }


    /** Por el repositorio: el gestor no deja programar en el pasado. */
    private int funcionQueYaEmpezo() {
        return funcionRepository.save(new Funcion(1, 1, reloj.ahora().minusMinutes(30),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000))).getId();
    }

    @Test
    void noSeReservaUnaFuncionQueYaEmpezo() {
        int empezada = funcionQueYaEmpezo();

        assertThrows(IllegalArgumentException.class,
                () -> reservas.reservar(empezada, 1, generales("A1")));
    }

    @Test
    void laMismaButacaSeVendeParaUnaFuncionFutura() {
        funcionQueYaEmpezo();

        assertEquals(1, reservas.reservar(1, 1, generales("A1")).getCantidadEntradas());
    }

    /**
     * R19: reservó a tiempo y llega a la boletería tarde. La reserva va por el repositorio
     * porque el gestor ya rechaza reservar una función empezada.
     */
    @Test
    void noSeCobraUnaReservaCuyaFuncionYaEmpezo() {
        int empezada = funcionQueYaEmpezo();
        Asiento butaca = asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(1).get(0);
        Reserva reserva = reservaRepository.save(new Reserva(empezada, 1,
                List.of(new Entrada(butaca, TipoTarifa.GENERAL, Dinero.de(5000))),
                // creada recién: si fuera vieja saltaría R17 y no estaríamos probando R19
                reloj.ahora()));

        assertThrows(IllegalArgumentException.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    /** El mínimo para que ningún filtro devuelva la lista entera por casualidad. */
    private void cargarReservas() {
        clientes.registrar("Sofía Pérez", "sofia@ejemplo.com");
        cartelera.agregar("El Padrino", 175, List.of(Genero.DRAMA), Clasificacion.MAS_16);
        funciones.programar(2, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        reservas.reservar(1, 1, generales("A1", "A2"));   // Andrei, Matrix, el 20
        reservas.reservar(2, 2, generales("B1"));          // Sofía, El Padrino, el 21
        // Andrei queda con una vigente y una cancelada: combinar estado + texto importa.
        reservas.cancelar(reservas.reservar(1, 1, generales("A3")).getId());
    }

    @Test
    void buscarSinCriteriosDevuelveTodo() {
        cargarReservas();

        assertEquals(3, reservas.buscar(CriteriosReserva.ninguno()).size());
        assertEquals(3, reservas.buscar(null).size(), "null no puede romper: es 'sin filtros'");
    }

    @Test
    void filtraPorEstado() {
        cargarReservas();

        assertEquals(2, reservas.buscar(
                new CriteriosReserva(EstadoReserva.RESERVADA, null, null)).size());
        assertEquals(1, reservas.buscar(
                new CriteriosReserva(EstadoReserva.CANCELADA, null, null)).size());
    }

    /** El día es el de la función, no el de cuándo se hizo la reserva. */
    @Test
    void filtraPorElDiaDeLaFuncion() {
        cargarReservas();

        assertEquals(2, reservas.buscar(
                new CriteriosReserva(null, LocalDate.of(2026, 8, 20), null)).size());
        assertEquals(1, reservas.buscar(
                new CriteriosReserva(null, LocalDate.of(2026, 8, 21), null)).size());
        assertTrue(reservas.buscar(
                new CriteriosReserva(null, LocalDate.of(2026, 8, 22), null)).isEmpty());
    }

    /** Cada campo vive en una tabla distinta: por eso el criterio no es un WHERE. */
    @Test
    void elTextoBuscaPorClienteEmailPeliculaYButaca() {
        cargarReservas();

        assertEquals(2, buscarTexto("andrei").size(), "por nombre del cliente");
        assertEquals(1, buscarTexto("sofia@ejemplo").size(), "por email");
        assertEquals(1, buscarTexto("padrino").size(), "por título de la película");
        assertEquals(1, buscarTexto("B1").size(), "por código de butaca");
    }

    @Test
    void elTextoNoDistingueMayusculasYEsParcial() {
        cargarReservas();

        assertEquals(2, buscarTexto("ANDREI").size());
        assertEquals(2, buscarTexto("ndre").size(), "coincide en el medio");
    }

    /** El código del ticket es lo único que trae quien perdió el mail. */
    @Test
    void elTextoBuscaPorCodigoDeReserva() {
        cargarReservas();
        String codigo = reservas.buscar(CriteriosReserva.ninguno()).get(0).getCodigo();

        assertEquals(1, buscarTexto(codigo).size());
        assertEquals(1, buscarTexto(codigo.toLowerCase()).size());
    }

    /** Los criterios se acumulan: Andrei tiene dos, pero una sola sigue vigente. */
    @Test
    void losCriteriosSeCombinan() {
        cargarReservas();

        assertEquals(1, reservas.buscar(
                new CriteriosReserva(EstadoReserva.RESERVADA, null, "andrei")).size());
        assertEquals(1, reservas.buscar(
                new CriteriosReserva(EstadoReserva.CANCELADA, null, "andrei")).size());
    }

    @Test
    void unTextoEnBlancoNoFiltra() {
        cargarReservas();

        assertEquals(3, buscarTexto("").size());
        assertEquals(3, buscarTexto("   ").size(), "solo espacios es lo mismo que vacío");
    }

    @Test
    void buscarSinCoincidenciasDevuelveVacioYNoFalla() {
        cargarReservas();

        assertTrue(buscarTexto("nadie con ese nombre").isEmpty());
    }

    private List<Reserva> buscarTexto(String texto) {
        return reservas.buscar(new CriteriosReserva(null, null, texto));
    }

    private static Map<String, TipoTarifa> generales(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return butacas;
    }

    private static Dinero precioDe(Reserva reserva, String codigo) {
        return buscarEntrada(reserva, codigo).precio();
    }

    private static TipoTarifa tarifaDe(Reserva reserva, String codigo) {
        return buscarEntrada(reserva, codigo).tarifa();
    }

    private static Entrada buscarEntrada(Reserva reserva, String codigo) {
        return reserva.getEntradas().stream()
                .filter(e -> e.codigoAsiento().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La reserva no tiene la butaca " + codigo));
    }
}
