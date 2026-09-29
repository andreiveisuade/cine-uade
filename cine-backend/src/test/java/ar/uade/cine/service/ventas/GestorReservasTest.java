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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.ButacaOcupada;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.salas.AsientoRepository;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;

class GestorReservasTest extends PruebaDeIntegracion {

    private static final Path TICKETS = Path.of("target/comprobantes/tickets");

    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorAcceso acceso;
    @Autowired
    private ConsultasReservas consultas;
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
    private ClienteRepository clienteRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private LocalDateTime ahora;

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
        reservas.reservar(1, 1, generales("A1", "A2"), null);

        assertEquals(8, asientosLibres(1, null).size());
        assertTrue(asientosLibres(1, null).stream().noneMatch(a -> a.getCodigo().equals("A1")));
        assertTrue(asientosLibres(1, null).stream().anyMatch(a -> a.getCodigo().equals("A3")));
    }

    // 409 y no 400, como la carrera perdida en el flush: la web vuelve al mapa solo ante un 409.
    @Test
    void rechazaButacaYaOcupada() {
        reservas.reservar(1, 1, generales("B3"), null);

        ButacaOcupada error = assertThrows(ButacaOcupada.class,
                () -> reservas.reservar(1, 1, generales("B3"), null));

        assertEquals("La butaca B3 ya está ocupada", error.getMessage());
    }

    @Test
    void rechazaButacaInexistente() {
        assertThrows(Rechazo.class, () -> reservas.reservar(1, 1, generales("Z9"), null));
    }

    @Test
    void reservarDaDeAltaAlClienteQueNoExistia() {
        Reserva reserva = reservas.reservar(1, "Nueva", "nueva@uade.edu.ar", generales("A1"), null);

        assertEquals(clientes.buscarPorEmail("nueva@uade.edu.ar").orElseThrow().getId(), reserva.getClienteId());
    }

    @Test
    void unaReservaRechazadaNoDejaAlClienteDadoDeAlta() {
        assertThrows(Rechazo.class,
                () -> reservas.reservar(1, "Nueva", "nueva@uade.edu.ar", generales("Z9"), null));

        assertTrue(clientes.buscarPorEmail("nueva@uade.edu.ar").isEmpty());
    }

    @Test
    void sinButacasNoHayReserva() {
        Rechazo vacio = assertThrows(Rechazo.class,
                () -> reservas.reservar(1, 1, Map.of(), null));
        Rechazo sinCampo = assertThrows(Rechazo.class,
                () -> reservas.reservar(1, 1, null, null));

        assertEquals("Hay que elegir al menos una butaca", vacio.getMessage());
        assertEquals("Hay que elegir al menos una butaca", sinCampo.getMessage());
    }

    // Once butacas libres de una sala grande: el tope es de la compra, no de la sala.
    @Test
    void unaCompraNoLlevaMasDeDiezButacas() {
        salas.agregar("Sala grande", TipoSala.DOS_D, List.of(12));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        Map<String, TipoTarifa> once = generales("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11");

        Rechazo error = assertThrows(Rechazo.class, () -> reservas.reservar(2, 1, once, null));

        assertEquals("Una compra tiene que tener como máximo 10 butacas", error.getMessage());
        once.remove("A11");
        assertEquals(10, reservas.reservar(2, 1, once, null).getCantidadEntradas());
    }

    @Test
    void laMismaButacaDosVecesEsUnaSolaEntrada() {
        assertEquals(1, reservas.reservar(1, 1, generales("A1", "A1"), null).getCantidadEntradas());
    }

    // "a1" y "A1" son dos claves del pedido y una sola butaca: antes llegaban dos entradas al
    // UNIQUE y salía el 409 de "alguien tomó una de esas butacas", que no era lo que pasaba.
    @Test
    void laMismaButacaEscritaDeDosManerasSeRechazaComoRepetida() {
        Rechazo error = assertThrows(Rechazo.class,
                () -> reservas.reservar(1, 1, generales("a1", "A1"), null));

        assertEquals("La butaca A1 está repetida en el pedido", error.getMessage());
    }

    @Test
    void laTarifaReducidaAbarataSoloSuButaca() {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        butacas.put("A1", TipoTarifa.GENERAL);
        butacas.put("A2", TipoTarifa.JUBILADO);

        Reserva reserva = reservas.reservar(1, 1, butacas, null);
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

        Reserva reserva = reservas.reservar(1, 1, butacas, null);

        assertEquals(TipoTarifa.MENOR, tarifaDe(reserva, "A1"));
        assertEquals(TipoTarifa.ESTUDIANTE, tarifaDe(reserva, "A2"));
    }

    @Test
    void soloLaTarifaGeneralNoPideAcreditacion() {
        assertFalse(TipoTarifa.GENERAL.requiereAcreditacion());
        assertTrue(TipoTarifa.JUBILADO.requiereAcreditacion());
        assertTrue(TipoTarifa.MENOR.requiereAcreditacion());
        assertTrue(TipoTarifa.ESTUDIANTE.requiereAcreditacion());
    }

    @Test
    void cancelarLiberaLasButacas() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2", "A3"), null);
        reservas.cancelar(reserva.getId());

        assertEquals(10, asientosLibres(1, null).size());
        assertTrue(asientosLibres(1, null).stream().anyMatch(a -> a.getCodigo().equals("A1")));
    }

    @Test
    void noSePuedeCancelarUnaReservaYaCobrada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        Rechazo error = assertThrows(Rechazo.class,
                () -> reservas.cancelar(reserva.getId()));

        assertEquals("La reserva está pagada: solo se puede cancelar una reserva sin cobrar",
                error.getMessage());
        assertEquals(9, asientosLibres(1, null).size(), "la butaca cobrada sigue ocupada");
    }

    @Test
    void noSeCancelaDosVecesLaMismaReserva() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        reservas.cancelar(reserva.getId());

        assertThrows(Rechazo.class, () -> reservas.cancelar(reserva.getId()));
    }

    @Test
    void unaButacaFueraDeServicioNoSePuedeReservar() {
        salas.marcarFueraDeServicio(1, "A3");

        assertEquals(9, asientosLibres(1, null).size());
        assertTrue(asientosLibres(1, null).stream().noneMatch(a -> a.getCodigo().equals("A3")));
        assertThrows(Rechazo.class, () -> reservas.reservar(1, 1, generales("A3"), null));
    }

    @Test
    void reponerLaButacaLaVuelveAHabilitar() {
        salas.marcarFueraDeServicio(1, "A3");
        salas.reponer(1, "A3");

        assertEquals(10, asientosLibres(1, null).size());
        assertEquals(1, reservas.reservar(1, 1, generales("A3"), null).getCantidadEntradas());
    }

    @Test
    void emiteElTicketConLasButacas() throws IOException {
        Reserva reserva = reservas.reservar(1, 1, generales("B4", "B5"), null);

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
        salas.agregar("Sala 2", TipoSala.IMAX, List.of(4), Map.of("A1", TipoAsiento.VIP));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5000));

        Reserva vip = reservas.reservar(2, 1, generales("A1"), null);
        assertEquals(Dinero.de(12000.0), vip.getTotal());

        Reserva estandar = reservas.reservar(2, 1, generales("A2"), null);
        assertEquals(Dinero.de(8000.0), estandar.getTotal());
    }

    @Test
    void elRecargoPremiumSeAplicaUnaSolaVez() {
        salas.agregar("Sala VIP", TipoSala.IMAX, List.of(2), Map.of("A1", TipoAsiento.VIP));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 22, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));

        assertEquals(Dinero.de(12000.0), reservas.reservar(2, 1, generales("A1"), null).getTotal());
    }

    @Test
    void elPrecioSeRedondeaADosDecimales() {
        salas.agregar("Sala 3D", TipoSala.TRES_D, List.of(3));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 23, 20, 0),
                Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5250.50));

        assertEquals(Dinero.de(6825.65), reservas.reservar(2, 1, generales("A1"), null).getTotal());
    }

    @Test
    void guardaCuandoSeHizoLaReserva() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);

        assertEquals(reloj.hoy(), reserva.getCreadaEn().toLocalDate());
    }

    @Test
    void noSePuedeProgramar3DEnUnaSalaQueNoLoSoporta() {
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(4));
        assertThrows(Rechazo.class,
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 24, 20, 0),
                        Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5000)));
    }

    @Test
    void loGuardadoSeReleeConSusButacasYSuFecha() {
        reservas.reservar(1, 1, generales("A1", "B2"), null);

        Reserva leida = reservaRepository.findById(1).orElseThrow();

        assertEquals(2, leida.getCantidadEntradas());
        assertEquals("A1", leida.getEntradas().get(0).codigoAsiento());
        assertEquals(reloj.hoy(), leida.getCreadaEn().toLocalDate());
    }

    private void envejecer(int reservaId, int minutos) {
        jdbc.update("UPDATE reserva SET creada_en = ? WHERE id = ?",
                reservaRepository.findById(reservaId).orElseThrow()
                        .getCreadaEn().minusMinutes(minutos),
                reservaId);
    }

    @Test
    void unaReservaSinPagarVencidaLiberaSusButacas() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1", "A2"), null);
        assertEquals(8, asientosLibres(1, null).size());

        envejecer(reserva.getId(), Reserva.MINUTOS_PARA_PAGAR + 1);

        assertEquals(10, asientosLibres(1, null).size(), "las butacas vuelven a la venta");
        assertEquals(EstadoReserva.EXPIRADA, consultas.obtener(reserva.getId()).getEstado());
    }

    @Test
    void laReservaVencidaSeCierraReciénCuandoAlguienConsulta() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        envejecer(reserva.getId(), Reserva.MINUTOS_PARA_PAGAR + 1);

        assertEquals(EstadoReserva.RESERVADA, reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
        ocupacion.asientosOcupados(1, null);
        assertEquals(EstadoReserva.EXPIRADA, reservaRepository.findById(reserva.getId()).orElseThrow().getEstado());
    }

    @Test
    void unaReservaPagadaNoVence() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        envejecer(reserva.getId(), Reserva.MINUTOS_PARA_PAGAR + 1);

        assertEquals(9, asientosLibres(1, null).size(), "la butaca cobrada sigue ocupada");
    }

    @Test
    void noSeCobraUnaReservaVencida() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        envejecer(reserva.getId(), Reserva.MINUTOS_PARA_PAGAR + 1);

        assertThrows(Rechazo.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    // El estado con su etiqueta, no con la constante (EXPIRADA) que viaja en el JSON.
    @Test
    void unaReservaYaExpiradaDiceQueEstaVencida() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        envejecer(reserva.getId(), Reserva.MINUTOS_PARA_PAGAR + 1);
        ocupacion.asientosOcupados(1, null);

        Rechazo error = assertThrows(Rechazo.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));

        assertEquals("La reserva está vencida: no se puede cobrar", error.getMessage());
    }

    @Test
    void elCodigoNoEsElIdYNoSeRepite() {
        Reserva primera = reservas.reservar(1, 1, generales("A1"), null);
        Reserva segunda = reservas.reservar(1, 1, generales("A2"), null);

        assertNotEquals(primera.getCodigo(), segunda.getCodigo());
        assertNotEquals(String.valueOf(primera.getId()), primera.getCodigo());
        assertEquals(8, primera.getCodigo().length());
    }

    @Test
    void seIngresaUnaSolaVezYSoloSiEstaPagada() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        Rechazo sinPagar = assertThrows(Rechazo.class,
                () -> acceso.registrarIngreso(reserva.getCodigo()), "sin pagar no entra");

        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        reloj.mover(LocalDateTime.of(2026, 8, 20, 19, 42, 12, 345_678_000));
        assertNotNull(acceso.registrarIngreso(reserva.getCodigo()).getIngresadaEn());

        Rechazo dosVeces = assertThrows(Rechazo.class,
                () -> acceso.registrarIngreso(reserva.getCodigo()), "no entra dos veces");

        assertEquals("La reserva está sin pagar: solo se ingresa con una reserva pagada",
                sinPagar.getMessage());
        // La hora como la lee el acomodador, no el toString de LocalDateTime (2026-08-20T19:42:12.345678).
        assertEquals("Esa entrada ya se usó el 20/08 19:42", dosVeces.getMessage());
    }

    // Con la entrada del 20/08 no se pasa el 19 ni el 21, y el rechazo no la marca como usada.
    @Test
    void seIngresaSoloElDiaDeLaFuncion() {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        reloj.mover(LocalDateTime.of(2026, 8, 19, 23, 59));
        Rechazo antes = assertThrows(Rechazo.class, () -> acceso.registrarIngreso(reserva.getCodigo()));
        reloj.mover(LocalDateTime.of(2026, 8, 20, 0, 0));
        acceso.registrarIngreso(reserva.getCodigo());

        assertEquals("La función es el 20/08: se entra solo ese día", antes.getMessage());
        assertNotNull(reservaRepository.findById(reserva.getId()).orElseThrow().getIngresadaEn());
    }

    @Test
    void unCodigoInventadoNoAbreLaPuerta() {
        assertThrows(Rechazo.class, () -> acceso.registrarIngreso("XXXXXXXX"));
    }

    // El código es la única credencial del cliente: con leer el log no se tiene que poder entrar.
    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void elIngresoNoDejaElCodigoDeAccesoEnElLog(CapturedOutput log) {
        Reserva reserva = reservas.reservar(1, 1, generales("A1"), null);
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");
        reloj.mover(LocalDateTime.of(2026, 8, 20, 19, 30));

        acceso.registrarIngreso(reserva.getCodigo());

        assertTrue(log.getOut().contains("ingreso reserva " + reserva.getId()), "el ingreso se sigue logueando");
        assertFalse(log.getAll().contains(reserva.getCodigo()));
    }

    // Por el repositorio: el gestor no deja programar en el pasado.
    private Funcion funcionQueYaEmpezo() {
        return funcionRepository.save(new Funcion(cartelera.buscar(1).orElseThrow(),
                salas.buscar(1).orElseThrow(), reloj.ahora().minusMinutes(30),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }

    @Test
    void noSeReservaUnaFuncionQueYaEmpezo() {
        int empezada = funcionQueYaEmpezo().getId();

        assertThrows(Rechazo.class,
                () -> reservas.reservar(empezada, 1, generales("A1"), null));
    }

    @Test
    void laMismaButacaSeVendeParaUnaFuncionFutura() {
        funcionQueYaEmpezo();

        assertEquals(1, reservas.reservar(1, 1, generales("A1"), null).getCantidadEntradas());
    }

    @Test
    void noSeCobraUnaReservaCuyaFuncionYaEmpezo() {
        Funcion empezada = funcionQueYaEmpezo();
        Asiento butaca = asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(1).get(0);
        Reserva reserva = reservaRepository.save(new Reserva(empezada, clienteRepository.exigir(1, "el cliente"),
                List.of(new Entrada(butaca, TipoTarifa.GENERAL, Dinero.de(5000))),
                // creada recién: si fuera vieja saltaría R17 y no estaríamos probando R19
                reloj.ahora()));

        assertThrows(Rechazo.class,
                () -> pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, ""));
    }

    private void cargarReservas() {
        clientes.registrar("Sofía Pérez", "sofia@ejemplo.com");
        cartelera.agregar("El Padrino", 175, List.of(Genero.DRAMA), Clasificacion.MAS_16);
        funciones.programar(2, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        reservas.reservar(1, 1, generales("A1", "A2"), null);
        reservas.reservar(2, 2, generales("B1"), null);
        reservas.cancelar(reservas.reservar(1, 1, generales("A3"), null).getId());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin criterios devuelve todo,                 ,          ,           ,                     3
            reservadas,                                  RESERVADA, ,           ,                     2
            canceladas,                                  CANCELADA, ,           ,                     1
            el día de dos funciones,                     ,          2026-08-20, ,                     2
            el día de una función,                       ,          2026-08-21, ,                     1
            un día sin funciones,                        ,          2026-08-22, ,                     0
            por nombre del cliente,                      ,          ,           andrei,               2
            por email,                                   ,          ,           sofia@ejemplo,        1
            por título de la película,                   ,          ,           padrino,              1
            por código de butaca,                        ,          ,           B1,                   1
            el texto no distingue mayúsculas,            ,          ,           ANDREI,               2
            coincide en el medio,                        ,          ,           ndre,                 2
            reservadas de andrei,                        RESERVADA, ,           andrei,               1
            canceladas de andrei,                        CANCELADA, ,           andrei,               1
            un texto vacío no filtra,                    ,          ,           '',                   3
            solo espacios es lo mismo que vacío,         ,          ,           '   ',                3
            sin coincidencias devuelve vacío y no falla, ,          ,           nadie con ese nombre, 0
            """)
    void buscarFiltraPorEstadoDiaYTexto(String caso, EstadoReserva estado, LocalDate dia, String texto,
            int esperadas) {
        cargarReservas();

        assertEquals(esperadas, consultas.buscar(new CriteriosReserva(estado, dia, texto)).size(), caso);
    }

    @Test
    void buscarConNullEsSinFiltros() {
        cargarReservas();

        assertEquals(3, consultas.buscar(null).size(), "null no puede romper: es 'sin filtros'");
    }

    @Test
    void elTextoBuscaPorCodigoDeReserva() {
        cargarReservas();
        String codigo = consultas.buscar(new CriteriosReserva(null, null, null)).get(0).getCodigo();

        assertEquals(1, buscarTexto(codigo).size());
        assertEquals(1, buscarTexto(codigo.toLowerCase()).size());
    }

    @Test
    void lasDeUnEmailVanDeLaMasNuevaALaMasVieja() {
        cargarReservas();

        List<Reserva> deAndrei = consultas.listarPorEmail("  andrei@uade.edu.ar ");

        assertEquals(List.of(3, 1), deAndrei.stream().map(Reserva::getId).toList());
        assertEquals(List.of(3, 1), consultas.listarPorEmail("Andrei@UADE.edu.ar").stream()
                .map(Reserva::getId).toList(), "sin distinguir mayúsculas");
        assertTrue(consultas.listarPorEmail(null).isEmpty());
        assertTrue(consultas.listarPorEmail("   ").isEmpty());
        assertTrue(consultas.listarPorEmail("nadie@uade.edu.ar").isEmpty());
    }

    private List<Reserva> buscarTexto(String texto) {
        return consultas.buscar(new CriteriosReserva(null, null, texto));
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

    // Lo que ve el mapa de butacas (VistasCartelera#funcionConButacas): las de la sala menos las ocupadas.
    private List<Asiento> asientosLibres(int funcionId, String sesion) {
        int salaId = funciones.buscar(funcionId).orElseThrow().getSalaId();
        return Ocupacion.libresEntre(salas.asientosDe(salaId), ocupacion.asientosOcupados(funcionId, sesion));
    }
}
