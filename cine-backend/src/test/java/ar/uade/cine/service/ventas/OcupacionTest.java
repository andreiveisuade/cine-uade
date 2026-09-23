package ar.uade.cine.service.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.ventas.BloqueoButacaRepository;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;

class OcupacionTest extends PruebaDeIntegracion {

    private static final String ANA = "sesion-de-ana";
    private static final String BETO = "sesion-de-beto";

    @Autowired
    private Ocupacion ocupacion;
    @Autowired
    private GestorReservas reservas;
    @Autowired
    private BloqueoButacaRepository bloqueos;

    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorClientes clientes;

    private LocalDateTime ahora;

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

        ocupacion.liberar(1, BETO);

        assertFalse(codigosLibres(BETO).contains("A1"));
        ocupacion.liberar(1, ANA);
        assertTrue(codigosLibres(BETO).contains("A1"), "la dueña sí la suelta");
    }

    @Test
    void renovarLaPropiaNoDuplicaLaFila() {
        ocupacion.bloquear(1, List.of("A1"), ANA);
        ocupacion.bloquear(1, List.of("A1"), ANA);

        assertEquals(1, bloqueos.count());
    }

    @Test
    void laLimpiezaBorraSoloLosVencidos() {
        ocupacion.bloquear(1, List.of("A1"), ANA);
        avanzar(Duration.ofMinutes(2));
        ocupacion.bloquear(1, List.of("A2"), BETO);
        avanzar(Duration.ofMinutes(2));

        ocupacion.borrarBloqueosVencidos();

        assertEquals(1, bloqueos.count(), "el de Ana venció, el de Beto no");
        assertFalse(codigosLibres(ANA).contains("A2"));
    }

    @Test
    void borrarLaFuncionSeLlevaSusBloqueos() {
        ocupacion.bloquear(1, List.of("A1"), ANA);

        funciones.eliminar(1);

        assertEquals(0, bloqueos.count());
    }

    @Test
    void unaSesionMasLargaQueLaColumnaSeRechazaConMensaje() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ocupacion.bloquear(1, List.of("A1"), "x".repeat(65)));

        assertEquals("La sesión no puede tener más de 64 caracteres", error.getMessage());
    }

    @Test
    void muchasSesionesALaVezPorLaMismaButacaSeLaLlevaUnaSola() throws Exception {
        assertEquals(1, ganadoresDeUnaCarrera(), "una sola la consigue");
        assertEquals(1, bloqueos.count());
    }

    // Otro camino: la fila existe y está vencida, así que compiten en el UPDATE y no en el INSERT.
    @Test
    void muchasSesionesALaVezPorUnaButacaVencidaSeLaLlevaUnaSola() throws Exception {
        ocupacion.bloquear(1, List.of("A1"), "sesion-que-abandono");
        avanzar(Ocupacion.MIENTRAS_ELIGE.plusSeconds(1));

        assertEquals(1, ganadoresDeUnaCarrera(), "una sola la consigue");
        assertEquals(1, bloqueos.count());
    }

    private int ganadoresDeUnaCarrera() throws Exception {
        int competidores = 8;
        ExecutorService hilos = Executors.newFixedThreadPool(competidores);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<List<String>>> resultados = new ArrayList<>();
            for (int i = 0; i < competidores; i++) {
                String sesion = "sesion-" + i;
                Callable<List<String>> intento = () -> {
                    largada.await();
                    return ocupacion.bloquear(1, List.of("A1"), sesion);
                };
                resultados.add(hilos.submit(intento));
            }
            largada.countDown();
            int ganadores = 0;
            for (Future<List<String>> resultado : resultados) {
                if (resultado.get().equals(List.of("A1"))) {
                    ganadores++;
                }
            }
            return ganadores;
        } finally {
            hilos.shutdownNow();
        }
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

    @Test
    void confirmarLaReservaSueltaLosBloqueosDeEsaSesion() {
        ocupacion.bloquear(1, List.of("A1", "A2"), ANA);
        reservas.reservar(1, 1, generales("A1"), ANA);

        assertEquals(List.of("A2"), ocupacion.bloquear(1, List.of("A2"), BETO),
                "la que no compró vuelve a la venta");
        assertEquals(List.of(), ocupacion.bloquear(1, List.of("A1"), BETO),
                "la que compró sigue ocupada, ahora por la reserva");
    }

    private List<String> codigosLibres(String sesion) {
        return ocupacion.asientosLibres(1, sesion).stream().map(Asiento::getCodigo).toList();
    }

    private static Map<String, TipoTarifa> generales(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return butacas;
    }
}
