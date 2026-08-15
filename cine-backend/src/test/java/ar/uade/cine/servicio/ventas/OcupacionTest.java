package ar.uade.cine.servicio.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.BloqueoButacasMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;
import ar.uade.cine.persistencia.redis.BloqueoButacasRedis;
import ar.uade.cine.servicio.cartelera.GestorCartelera;
import ar.uade.cine.servicio.funciones.GestorFunciones;
import ar.uade.cine.servicio.programaciones.GestorProgramaciones;
import ar.uade.cine.servicio.salas.GestorSalas;
import ar.uade.cine.servicio.usuarios.GestorClientes;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * El bloqueo de butacas mientras alguien las está eligiendo: la etapa anterior a que
 * exista una reserva.
 *
 * <p>Lo que se prueba acá es que esa etapa entra en la <strong>misma</strong> definición de
 * "ocupado" que ya usaban el mapa y la venta —si quedara afuera, el mapa ofrecería una
 * butaca que la reserva después rechaza— y que sigue siendo una comodidad y no una
 * garantía: sin el medio donde vive el bloqueo, el sistema vende igual.
 *
 * <p>Corre sin Redis levantado, incluido el caso que justamente prueba que no hace falta.
 */
class OcupacionTest {

    @TempDir
    Path tempDir;

    /** Dos sesiones distintas eligiendo la misma función, que es el conflicto a probar. */
    private static final String ANA = "sesion-de-ana";
    private static final String BETO = "sesion-de-beto";

    private Ocupacion ocupacion;
    private GestorReservas reservas;
    private BloqueoButacasMemoria bloqueos;

    /**
     * El reloj de los bloqueos, movible a mano. Es el mismo criterio que
     * {@link ar.uade.cine.dominio.ventas.Reserva#estaVencida(LocalDateTime)}: probar que
     * algo vence no puede costar esperar a que venza, y una espera real vuelve al test
     * dependiente de lo cargada que esté la máquina.
     */
    private LocalDateTime ahora;

    private ReservaDAO reservaDAO;
    private FuncionDAO funcionDAO;
    private AsientoDAO asientoDAO;
    private SalaDAO salaDAO;
    private ClienteDAO clienteDAO;
    private PeliculaDAO peliculaDAO;

    /** Sala de 2 filas x 5 butacas (A1..A5, B1..B5), una función a $5000, un cliente. */
    @BeforeEach
    void prepararEscenario() {
        peliculaDAO = new PeliculaDAOMemoria();
        salaDAO = new SalaDAOMemoria();
        asientoDAO = new AsientoDAOMemoria();
        funcionDAO = new FuncionDAOMemoria();
        clienteDAO = new ClienteDAOMemoria();
        reservaDAO = new ReservaDAOMemoria();

        GestorFunciones funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        new GestorCartelera(peliculaDAO, funcionDAO, new GestorProgramaciones(
                new ProgramacionDAOMemoria(), funcionDAO, funciones))
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        new GestorSalas(salaDAO, asientoDAO, funcionDAO).agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 12, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        new GestorClientes(clienteDAO, reservaDAO, new CompraCandyDAOMemoria())
                .registrar("Andrei", "andrei@uade.edu.ar");

        ahora = LocalDateTime.of(2026, 8, 14, 10, 0);
        bloqueos = new BloqueoButacasMemoria(() -> ahora);
        ocupacion = new Ocupacion(reservaDAO, funcionDAO, asientoDAO, bloqueos);
        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO,
                peliculaDAO, new GeneradorTicketTxt(tempDir.resolve("tickets")),
                new CalculadoraPrecio(), ocupacion);
    }

    // ---------- la butaca deja de ofrecerse ----------

    @Test
    void laButacaQueAlguienEstaEligiendoDejaDeAparecerLibre() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        assertEquals(9, ocupacion.lugaresLibres(1));
        assertFalse(codigosLibres(null).contains("A1"));
    }

    @Test
    void aQuienLaEstaEligiendoSiLeAparecelibre() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        assertTrue(codigosLibres(ANA).contains("A1"), "las suyas no le están ocupadas a ella");
        assertEquals(10, ocupacion.lugaresLibres(1, ANA));
        assertFalse(codigosLibres(BETO).contains("A1"), "pero al de al lado sí");
    }

    // ---------- dos por la misma butaca ----------

    @Test
    void dosPersonasPorLaMismaButacaSeLaLlevaLaPrimera() {
        assertEquals(List.of("A1"), ocupacion.bloquear(1, List.of("A1"), ANA));
        assertEquals(List.of(), ocupacion.bloquear(1, List.of("A1"), BETO));
    }

    /** Perder una butaca no invalida el resto del pedido: se contesta lo que sí se consiguió. */
    @Test
    void loQueNoSeConsigueNoArrastraAlResto() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        assertEquals(List.of("A2", "A3"), ocupacion.bloquear(1, List.of("A1", "A2", "A3"), BETO));
    }

    @Test
    void noSeBloqueaUnaButacaYaVendida() {
        reservas.reservar(1, 1, generales("A1"));

        assertEquals(List.of(), ocupacion.bloquear(1, List.of("A1"), ANA));
    }

    @Test
    void laButacaInexistenteFallaConElMismoMensajeQueAlReservar() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ocupacion.bloquear(1, List.of("Z9"), ANA));

        assertEquals("La butaca Z9 no existe en esa sala", error.getMessage());
    }

    // ---------- el vencimiento ----------

    @Test
    void elBloqueoVencidoDevuelveLaButacaALaVenta() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        ahora = ahora.plus(Ocupacion.MIENTRAS_ELIGE).plusSeconds(1);

        assertTrue(codigosLibres(null).contains("A1"));
        assertEquals(List.of("A1"), ocupacion.bloquear(1, List.of("A1"), BETO),
                "y el que llega después se la puede llevar");
    }

    @Test
    void volverATocarElMapaRenuevaElBloqueo() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        // Dos minutos después toca el mapa de nuevo: no perdió la butaca por tardar.
        ahora = ahora.plusMinutes(2);
        assertEquals(List.of("A1"), ocupacion.bloquear(1, List.of("A1"), ANA));

        // Cuatro minutos desde el primer bloqueo: sin la renovación ya habría vencido.
        ahora = ahora.plusMinutes(2);
        assertFalse(codigosLibres(BETO).contains("A1"));
    }

    @Test
    void deseleccionarUnaButacaLaDevuelveALaVenta() {
        ocupacion.bloquear(1, List.of("A1", "A2"), ANA);
        ocupacion.bloquear(1, List.of("A1"), ANA);

        assertEquals(List.of("A2"), ocupacion.bloquear(1, List.of("A2"), BETO));
    }

    @Test
    void soltarNoSirveParaSoltarLaDeOtro() {
        ocupacion.bloquear(1, List.of("A1"), ANA);
        int a1 = idDe("A1");

        bloqueos.liberar(1, a1, BETO);

        assertFalse(codigosLibres(BETO).contains("A1"));
    }

    // ---------- el paso a la reserva ----------

    @Test
    void elQueEligioPuedeReservarLoQueTieneBloqueado() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        assertEquals(1, reservas.reservar(1, 1, generales("A1"), ANA).getCantidadEntradas());
    }

    @Test
    void otroNoPuedeReservarLoQueAlguienEstaEligiendo() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> reservas.reservar(1, 1, generales("A1"), BETO));

        assertEquals("La butaca A1 ya está ocupada", error.getMessage());
    }

    /**
     * Las dos ventanas no se solapan: confirmada la reserva, la butaca la retiene ella y
     * el bloqueo sobra. Y lo que se miró sin comprar vuelve a la venta en el acto, sin
     * esperar a que venza.
     */
    @Test
    void confirmarLaReservaSueltaLosBloqueosDeEsaSesion() {
        ocupacion.bloquear(1, List.of("A1", "A2"), ANA);
        reservas.reservar(1, 1, generales("A1"), ANA);

        assertEquals(List.of("A2"), ocupacion.bloquear(1, List.of("A2"), BETO),
                "la que no compró vuelve a la venta");
        assertEquals(List.of(), ocupacion.bloquear(1, List.of("A1"), BETO),
                "la que compró sigue ocupada, ahora por la reserva");
    }

    // ---------- sin el medio donde vive el bloqueo ----------

    /**
     * Redis caído no puede voltear la venta: el bloqueo es comodidad, y la garantía de que
     * una butaca no se venda dos veces la sigue dando el UNIQUE de la base. El sistema
     * vuelve a comportarse como antes de que el bloqueo existiera.
     *
     * <p>El puerto no tiene nada escuchando a propósito: es la forma de probar la caída sin
     * tener que levantar Redis para después apagarlo.
     */
    @Test
    void sinRedisSeSigueVendiendoComoAntes() {
        Ocupacion sinRedis = new Ocupacion(reservaDAO, funcionDAO, asientoDAO,
                new BloqueoButacasRedis("127.0.0.1", 63999));

        GestorReservas ventaSinRedis = new GestorReservas(reservaDAO, funcionDAO, salaDAO,
                asientoDAO, clienteDAO, peliculaDAO,
                new GeneradorTicketTxt(tempDir.resolve("tickets")),
                new CalculadoraPrecio(), sinRedis);

        assertEquals(List.of("A1"), sinRedis.bloquear(1, List.of("A1"), ANA),
                "nadie la tiene tomada, así que se la lleva");
        assertEquals(10, sinRedis.lugaresLibres(1), "pero no queda anotada en ningún lado");
        assertEquals(1, ventaSinRedis.reservar(1, 1, generales("A1"), ANA).getCantidadEntradas(),
                "y la reserva sale igual");
    }

    // ---------- ayudas ----------

    private List<String> codigosLibres(String sesion) {
        return ocupacion.asientosLibres(1, sesion).stream().map(Asiento::getCodigo).toList();
    }

    private int idDe(String codigo) {
        return asientoDAO.listarPorSala(1).stream()
                .filter(a -> a.getCodigo().equals(codigo))
                .map(Asiento::getId)
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, TipoTarifa> generales(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return butacas;
    }
}
