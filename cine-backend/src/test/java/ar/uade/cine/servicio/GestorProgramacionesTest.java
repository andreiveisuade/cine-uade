package ar.uade.cine.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ProgramacionDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.persistencia.archivo.ReservaDAOTxt;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;

/**
 * La grilla que materializa funciones: que genere las del rango, que respete los días
 * elegidos, que previsualizar no escriba y que el alta saltee lo que choca contra R3.
 */
class GestorProgramacionesTest {

    @TempDir
    Path tempDir;

    private static final LocalDate LUNES = LocalDate.of(2026, 9, 7);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 9, 13);
    private static final LocalTime LAS_2030 = LocalTime.of(20, 30);

    private final PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
    private final SalaDAO salaDAO = new SalaDAOMemoria();
    private final FuncionDAO funcionDAO = new FuncionDAOMemoria();
    private final ProgramacionDAO programacionDAO = new ProgramacionDAOMemoria();

    private GestorFunciones funciones;
    private GestorProgramaciones programaciones;

    /** Matrix (136') en la Sala 1, que es 2D, y una Sala 2 que sí proyecta en 3D. */
    @BeforeEach
    void prepararCartelera() {
        ReservaDAO reservaDAO = new ReservaDAOTxt(tempDir.resolve("reservas.txt"));
        new GestorCartelera(peliculaDAO, funcionDAO, new GestorProgramaciones(
                programacionDAO, funcionDAO,
                new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO)))
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        GestorSalas salas = new GestorSalas(salaDAO, new AsientoDAOMemoria(), funcionDAO);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        salas.agregar("Sala 2", TipoSala.TRES_D, List.of(5, 5));

        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        programaciones = new GestorProgramaciones(programacionDAO, funcionDAO, funciones);
    }

    /** El caso del enunciado: una semana entera, una función por día. */
    @Test
    void generaUnaFuncionPorCadaDiaDelRango() {
        PlanProgramacion plan = crearSemana(Set.of());

        assertEquals(7, plan.funciones().size());
        assertEquals(7, funciones.listar().size());
        assertTrue(plan.salteadas().isEmpty());
        assertEquals(LocalDateTime.of(2026, 9, 7, 20, 30), plan.funciones().get(0).inicio());
        assertEquals(LocalDateTime.of(2026, 9, 13, 20, 30), plan.funciones().get(6).inicio());
    }

    /** Sin días elegidos corre toda la semana; con días, solo esos. */
    @Test
    void respetaLosDiasDeLaSemanaElegidos() {
        PlanProgramacion plan = crearSemana(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        assertEquals(2, plan.funciones().size());
        assertEquals(LocalDateTime.of(2026, 9, 12, 20, 30), plan.funciones().get(0).inicio());
        assertEquals(LocalDateTime.of(2026, 9, 13, 20, 30), plan.funciones().get(1).inicio());
    }

    /** Cada función generada sabe de qué grilla salió: es la asociación materializada. */
    @Test
    void lasFuncionesGeneradasApuntanASuProgramacion() {
        PlanProgramacion plan = crearSemana(Set.of());
        int id = plan.programacion().getId();

        assertEquals(7, programaciones.funcionesDe(id).size());
        assertTrue(funciones.listar().stream().allMatch(f -> f.getProgramacionId() == id));
    }

    /** Una función cargada a mano no pertenece a ninguna grilla. */
    @Test
    void laFuncionSueltaNoTieneProgramacion() {
        Funcion suelta = funciones.programar(1, 1, LocalDateTime.of(2026, 10, 1, 20, 30),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);

        assertNull(suelta.getProgramacionId());
    }

    // ---------- previsualizar y después aplicar ----------

    @Test
    void previsualizarNoEscribeNada() {
        PlanProgramacion plan = previsualizarSemana();

        assertEquals(7, plan.funciones().size());
        assertTrue(funciones.listar().isEmpty(), "previsualizar no puede guardar funciones");
        assertTrue(programaciones.listar().isEmpty(), "ni la grilla");
        assertEquals(0, plan.programacion().getId(), "la grilla previsualizada no tiene id");
    }

    /**
     * R3: el miércoles la sala ya está ocupada, así que esa fecha se marca y las otras
     * seis siguen. Rechazar la grilla entera por una fecha la haría inusable.
     */
    @Test
    void marcaLasFechasQueChocanYGeneraElResto() {
        funciones.programar(1, 1, LocalDateTime.of(2026, 9, 9, 21, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);

        PlanProgramacion plan = crearSemana(Set.of());

        assertEquals(1, plan.salteadas().size());
        assertEquals(LocalDateTime.of(2026, 9, 9, 20, 30), plan.salteadas().get(0).inicio());
        assertTrue(plan.salteadas().get(0).motivo().contains("09/09 21:00"),
                "el informe tiene que decir contra qué choca");
        assertEquals(6, plan.programables().size());
        // Las seis de la grilla más la que ya estaba cargada.
        assertEquals(7, funciones.listar().size());
    }

    /** La previsualización predice exactamente lo que hace el alta: es su único trabajo. */
    @Test
    void laPrevisualizacionCoincideConLoQueTerminaGenerando() {
        funciones.programar(1, 1, LocalDateTime.of(2026, 9, 9, 21, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);

        PlanProgramacion previo = previsualizarSemana();
        PlanProgramacion alta = crearSemana(Set.of());

        assertEquals(previo.programables().stream().map(f -> f.inicio()).toList(),
                alta.programables().stream().map(f -> f.inicio()).toList());
        assertEquals(previo.salteadas().size(), alta.salteadas().size());
    }

    /**
     * Lo que justifica revalidar al aplicar: entre la previsualización y el confirmar,
     * otro programó algo en esa sala. El alta no puede confiar en el informe viejo.
     */
    @Test
    void elAltaRevalidaYNoConfiaEnLaPrevisualizacion() {
        PlanProgramacion previo = previsualizarSemana();
        assertTrue(previo.salteadas().isEmpty(), "cuando se previsualizó, la sala estaba libre");

        funciones.programar(1, 1, LocalDateTime.of(2026, 9, 9, 21, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
        PlanProgramacion alta = crearSemana(Set.of());

        assertEquals(1, alta.salteadas().size(), "al aplicar, la fecha ya estaba tomada");
        assertEquals(6, alta.programables().size());
    }

    /** Dos grillas iguales: la segunda no puede duplicar ni una función. */
    @Test
    void laSegundaGrillaIgualNoGeneraNada() {
        crearSemana(Set.of());
        PlanProgramacion repetida = crearSemana(Set.of());

        assertEquals(7, repetida.salteadas().size());
        assertTrue(repetida.programables().isEmpty());
        assertEquals(7, funciones.listar().size());
        // La grilla igual se guarda: es una decisión del cine, y el informe dice qué pasó.
        assertEquals(2, programaciones.listar().size());
    }

    // ---------- baja ----------

    /**
     * Dar de baja la grilla no toca las funciones ya generadas: pueden tener entradas
     * vendidas. Lo que evita es que se generen nuevas.
     */
    @Test
    void laBajaNoTocaLasFuncionesYaGeneradas() {
        int id = crearSemana(Set.of()).programacion().getId();

        programaciones.desactivar(id);

        assertFalse(programaciones.buscar(id).orElseThrow().estaActiva());
        assertEquals(7, funciones.listar().size());
        assertEquals(7, programaciones.funcionesDe(id).size());
    }

    @Test
    void unaGrillaDadaDeBajaSePuedeVolverAActivar() {
        int id = crearSemana(Set.of()).programacion().getId();

        programaciones.desactivar(id);
        programaciones.activar(id);

        assertTrue(programaciones.buscar(id).orElseThrow().estaActiva());
    }

    @Test
    void noSeDaDeBajaUnaProgramacionQueNoExiste() {
        assertThrows(IllegalArgumentException.class, () -> programaciones.desactivar(99));
    }

    // ---------- lo que se valida una vez por grilla, no una por función ----------

    /**
     * R8 vale para la grilla entera: si la sala no proyecta en 3D, no hay ninguna fecha
     * del rango en la que sí. Tiene que fallar ya en la previsualización, o el
     * administrador vería siete funciones perfectas y recién explotaría al confirmar.
     */
    @Test
    void previsualizarFallaIgualQueElAltaSiLaSalaNoProyectaEn3D() {
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.previsualizar(1, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                        Version.DOBLADA, Proyeccion.TRES_D, 5000));
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(1, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                        Version.DOBLADA, Proyeccion.TRES_D, 5000));
        assertTrue(programaciones.listar().isEmpty(), "la grilla inválida no se guarda");
    }

    @Test
    void rechazaUnRangoQueTerminaAntesDeEmpezar() {
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(1, 1, DOMINGO, LUNES, LAS_2030, Set.of(),
                        Version.SUBTITULADA, Proyeccion.DOS_D, 5000));
    }

    @Test
    void rechazaPrecioCeroYPeliculaInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(1, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                        Version.SUBTITULADA, Proyeccion.DOS_D, 0));
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(99, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                        Version.SUBTITULADA, Proyeccion.DOS_D, 5000));
    }

    /** Una grilla de solo lunes sobre un rango de martes a jueves no generaría nada. */
    @Test
    void rechazaUnaGrillaQueNoCaeEnNingunDiaDelRango() {
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(1, 1, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 10),
                        LAS_2030, Set.of(DayOfWeek.MONDAY), Version.SUBTITULADA, Proyeccion.DOS_D, 5000));
    }

    // ---------- la grilla abierta, que rueda sola ----------

    /**
     * Sin {@code hasta}, el alta no puede generar "todo el rango": el rango no termina.
     * Genera hasta el horizonte y ahí se detiene.
     */
    @Test
    void unaGrillaAbiertaGeneraSoloHastaElHorizonte() {
        PlanProgramacion plan = crearAbierta();

        // 14 días de horizonte contados desde hoy, más el de hoy mismo si entra en el rango.
        assertFalse(plan.funciones().isEmpty(), "una grilla abierta tiene que generar algo");
        LocalDate ultima = plan.funciones().get(plan.funciones().size() - 1).inicio().toLocalDate();
        assertFalse(ultima.isAfter(LocalDate.now().plusDays(14)),
                "no puede generar más allá del horizonte");
    }

    /** Una grilla cerrada no se recorta: el administrador ya dijo hasta cuándo. */
    @Test
    void unaGrillaCerradaGeneraTodoSuRangoAunqueSeaLejano() {
        // El rango de LUNES a DOMINGO cae bastante más allá de dos semanas desde hoy.
        assertEquals(7, crearSemana(Set.of()).funciones().size());
    }

    /** Llamar a extender mil veces en el mismo día tiene que hacer trabajo una sola vez. */
    @Test
    void extenderEsIdempotente() {
        crearAbierta();
        int despuesDelAlta = funcionDAO.listar().size();

        assertEquals(0, programaciones.extenderActivas(LocalDate.now()));
        assertEquals(0, programaciones.extenderActivas(LocalDate.now()));
        assertEquals(despuesDelAlta, funcionDAO.listar().size());
    }

    /** El día que el horizonte se corre, aparecen las funciones nuevas y solo esas. */
    @Test
    void alPasarLosDiasExtiendeLasQueFaltan() {
        crearAbierta();
        int despuesDelAlta = funcionDAO.listar().size();

        int generadas = programaciones.extenderActivas(LocalDate.now().plusDays(3));

        assertEquals(3, generadas, "tres días más de horizonte son tres funciones más");
        assertEquals(despuesDelAlta + 3, funcionDAO.listar().size());
    }

    /**
     * Acá {@code activa} deja de ser decorativo. Antes, con toda grilla cerrada y generada
     * de una vez, dar de baja no evitaba ninguna función: ya estaban todas.
     */
    @Test
    void unaGrillaDadaDeBajaNoExtiende() {
        PlanProgramacion plan = crearAbierta();
        int despuesDelAlta = funcionDAO.listar().size();

        programaciones.desactivar(plan.programacion().getId());

        assertEquals(0, programaciones.extenderActivas(LocalDate.now().plusDays(30)));
        assertEquals(despuesDelAlta, funcionDAO.listar().size(),
                "las que ya generó siguen; nuevas no aparecen");
    }

    /** Reactivarla la pone al día de una vez: no perdió las fechas que no generó. */
    @Test
    void reactivarlaVuelveAExtender() {
        PlanProgramacion plan = crearAbierta();
        programaciones.desactivar(plan.programacion().getId());
        programaciones.extenderActivas(LocalDate.now().plusDays(10));

        programaciones.activar(plan.programacion().getId());

        assertEquals(10, programaciones.extenderActivas(LocalDate.now().plusDays(10)));
    }

    /** Una grilla cerrada ya generada no vuelve a generar por más que pase el tiempo. */
    @Test
    void unaGrillaCerradaNoSePasaDeSuHasta() {
        crearSemana(Set.of());
        int delAlta = funcionDAO.listar().size();

        assertEquals(0, programaciones.extenderActivas(DOMINGO.plusMonths(6)));
        assertEquals(delAlta, funcionDAO.listar().size());
    }

    // ---------- el buscador de grillas ----------

    /**
     * Dos grillas de la misma película en salas distintas, una de ellas dada de baja.
     * La de baja importa: es la que el filtro tiene que poder separar.
     */
    private void cargarGrillas() {
        crearSemana(Set.of());
        PlanProgramacion enSala2 = programaciones.crear(1, 2, LUNES, DOMINGO, LocalTime.of(23, 0),
                Set.of(), Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
        programaciones.desactivar(enSala2.programacion().getId());
    }

    @Test
    void buscarSinCriteriosDevuelveTodasIncluidasLasDeBaja() {
        cargarGrillas();

        assertEquals(2, programaciones.buscar(null, null, null).size());
    }

    /**
     * Es la pregunta que más se hace en esta pantalla: cuáles están generando funciones.
     * Las dadas de baja no se borran nunca —siguen explicando las que ya crearon— así que
     * la lista solo crece y sin este filtro se vuelve ilegible.
     */
    @Test
    void filtraLasActivasYLasDadasDeBaja() {
        cargarGrillas();

        assertEquals(1, programaciones.buscar(null, null, true).size());
        assertEquals(1, programaciones.buscar(null, null, false).size());
        assertEquals(2, programaciones.buscar(null, null, null).size(), "null es todas");
    }

    @Test
    void filtraPorSalaYCombinaConElEstado() {
        cargarGrillas();

        assertEquals(1, programaciones.buscar(null, 1, null).size());
        assertEquals(1, programaciones.buscar(null, 2, null).size());
        // La de la sala 2 es justamente la que está de baja.
        assertTrue(programaciones.buscar(null, 2, true).isEmpty());
        assertEquals(1, programaciones.buscar(null, 2, false).size());
    }

    @Test
    void buscarSinCoincidenciasDevuelveVacio() {
        cargarGrillas();

        assertTrue(programaciones.buscar(99, null, null).isEmpty());
    }

    // ---------- helpers ----------

    /** Matrix en la Sala 1 a las 20:30, desde hoy, hasta que alguien la dé de baja. */
    private PlanProgramacion crearAbierta() {
        return programaciones.crear(1, 1, LocalDate.now(), null, LAS_2030, Set.of(),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
    }

    private PlanProgramacion crearSemana(Set<DayOfWeek> dias) {
        return programaciones.crear(1, 1, LUNES, DOMINGO, LAS_2030, dias,
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
    }

    private PlanProgramacion previsualizarSemana() {
        return programaciones.previsualizar(1, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
    }
}
