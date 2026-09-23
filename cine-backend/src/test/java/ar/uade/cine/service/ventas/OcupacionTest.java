package ar.uade.cine.service.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacas;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasRedis;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.AsientoRepository;
import ar.uade.cine.repository.ClienteRepository;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.repository.SalaRepository;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;

/**
 * El bloqueo mientras se elige entra en la misma definición de "ocupado" que el mapa y la
 * venta, y es comodidad, no garantía: sin Redis se vende igual.
 */
class OcupacionTest extends PruebaDeIntegracion {

    private static final String ANA = "sesion-de-ana";
    private static final String BETO = "sesion-de-beto";

    @Autowired
    private Ocupacion ocupacion;
    @Autowired
    private GestorReservas reservas;
    @Autowired
    private BloqueoButacas bloqueos;

    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorClientes clientes;
    @Autowired
    private CalculadoraPrecio calculadoraPrecio;

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private FuncionRepository funcionRepository;
    @Autowired
    private AsientoRepository asientoRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private PeliculaRepository peliculaRepository;

    private LocalDateTime ahora;

    /** Sala de 2 filas x 5 butacas (A1..A5, B1..B5), una función a $5000, un cliente. */
    @BeforeEach
    void prepararEscenario() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 12, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        clientes.registrar("Andrei", "andrei@uade.edu.ar");

        ahora = LocalDateTime.of(2026, 8, 14, 10, 0);
        reloj.mover(ahora);
    }

    private void avanzar(Duration cuanto) {
        ahora = ahora.plus(cuanto);
        reloj.mover(ahora);
    }

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

    @Test
    void dosPersonasPorLaMismaButacaSeLaLlevaLaPrimera() {
        assertEquals(List.of("A1"), ocupacion.bloquear(1, List.of("A1"), ANA));
        assertEquals(List.of(), ocupacion.bloquear(1, List.of("A1"), BETO));
    }

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

    @Test
    void elBloqueoVencidoDevuelveLaButacaALaVenta() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        avanzar(Ocupacion.MIENTRAS_ELIGE.plusSeconds(1));

        assertTrue(codigosLibres(null).contains("A1"));
        assertEquals(List.of("A1"), ocupacion.bloquear(1, List.of("A1"), BETO),
                "y el que llega después se la puede llevar");
    }

    @Test
    void volverATocarElMapaRenuevaElBloqueo() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        avanzar(Duration.ofMinutes(2));
        assertEquals(List.of("A1"), ocupacion.bloquear(1, List.of("A1"), ANA));

        // Sin la renovación ya habría vencido.
        avanzar(Duration.ofMinutes(2));
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

    /** Confirmada la reserva, el bloqueo sobra; lo que no se compró vuelve sin esperar a que venza. */
    @Test
    void confirmarLaReservaSueltaLosBloqueosDeEsaSesion() {
        ocupacion.bloquear(1, List.of("A1", "A2"), ANA);
        reservas.reservar(1, 1, generales("A1"), ANA);

        assertEquals(List.of("A2"), ocupacion.bloquear(1, List.of("A2"), BETO),
                "la que no compró vuelve a la venta");
        assertEquals(List.of(), ocupacion.bloquear(1, List.of("A1"), BETO),
                "la que compró sigue ocupada, ahora por la reserva");
    }

    /**
     * La garantía contra la doble venta es el UNIQUE de la base, no Redis. El puerto no
     * tiene nada escuchando a propósito: simula la caída.
     */
    @Test
    void sinRedisSeSigueVendiendoComoAntes() {
        Ocupacion sinRedis = new Ocupacion(reservaRepository, funcionRepository,
                asientoRepository, new BloqueoButacasRedis("127.0.0.1", 63999), reloj);

        GestorReservas ventaSinRedis = new GestorReservas(reservaRepository, funcionRepository,
                salaRepository, asientoRepository, clienteRepository, clientes, peliculaRepository,
                new GeneradorTicketTxt(java.nio.file.Path.of("target/comprobantes/tickets")),
                calculadoraPrecio, sinRedis, reloj);

        assertEquals(List.of("A1"), sinRedis.bloquear(1, List.of("A1"), ANA),
                "nadie la tiene tomada, así que se la lleva");
        assertEquals(10, sinRedis.lugaresLibres(1), "pero no queda anotada en ningún lado");
        assertEquals(1, ventaSinRedis.reservar(1, 1, generales("A1"), ANA).getCantidadEntradas(),
                "y la reserva sale igual");
    }

    private List<String> codigosLibres(String sesion) {
        return ocupacion.asientosLibres(1, sesion).stream().map(Asiento::getCodigo).toList();
    }

    private int idDe(String codigo) {
        return asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(1).stream()
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
