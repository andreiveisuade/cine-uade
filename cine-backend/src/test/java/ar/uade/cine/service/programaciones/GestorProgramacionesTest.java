package ar.uade.cine.service.programaciones;

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
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.ProgramacionRepository;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.model.dinero.Dinero;

class GestorProgramacionesTest extends PruebaDeIntegracion {

    private static final LocalDate LUNES = LocalDate.of(2026, 9, 7);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 9, 13);
    private static final LocalTime LAS_2030 = LocalTime.of(20, 30);

    @Autowired
    private FuncionRepository funcionRepository;
    @Autowired
    private ProgramacionRepository programacionRepository;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorProgramaciones programaciones;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;

    @BeforeEach
    void prepararCartelera() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        salas.agregar("Sala 2", TipoSala.TRES_D, List.of(5, 5));
    }

    @Test
    void generaUnaFuncionPorCadaDiaDelRango() {
        PlanProgramacion plan = crearSemana(Set.of());

        assertEquals(7, plan.funciones().size());
        assertEquals(7, funciones.listar().size());
        assertTrue(plan.salteadas().isEmpty());
        assertEquals(LocalDateTime.of(2026, 9, 7, 20, 30), plan.funciones().get(0).inicio());
        assertEquals(LocalDateTime.of(2026, 9, 13, 20, 30), plan.funciones().get(6).inicio());
    }

    @Test
    void respetaLosDiasDeLaSemanaElegidos() {
        PlanProgramacion plan = crearSemana(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        assertEquals(2, plan.funciones().size());
        assertEquals(LocalDateTime.of(2026, 9, 12, 20, 30), plan.funciones().get(0).inicio());
        assertEquals(LocalDateTime.of(2026, 9, 13, 20, 30), plan.funciones().get(1).inicio());
    }

    @Test
    void lasFuncionesGeneradasApuntanASuProgramacion() {
        PlanProgramacion plan = crearSemana(Set.of());
        int id = plan.programacion().getId();

        assertEquals(7, programaciones.funcionesDe(id).size());
        assertTrue(funciones.listar().stream().allMatch(f -> f.getProgramacionId() == id));
    }

    @Test
    void laFuncionSueltaNoTieneProgramacion() {
        Funcion suelta = funciones.programar(1, 1, LocalDateTime.of(2026, 10, 1, 20, 30),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        assertNull(suelta.getProgramacionId());
    }

    @Test
    void previsualizarNoEscribeNada() {
        PlanProgramacion plan = previsualizarSemana();

        assertEquals(7, plan.funciones().size());
        assertTrue(funciones.listar().isEmpty(), "previsualizar no puede guardar funciones");
        assertTrue(programaciones.listar().isEmpty(), "ni la grilla");
        assertEquals(0, plan.programacion().getId(), "la grilla previsualizada no tiene id");
    }

    @Test
    void marcaLasFechasQueChocanYGeneraElResto() {
        funciones.programar(1, 1, LocalDateTime.of(2026, 9, 9, 21, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        PlanProgramacion plan = crearSemana(Set.of());

        assertEquals(1, plan.salteadas().size());
        assertEquals(LocalDateTime.of(2026, 9, 9, 20, 30), plan.salteadas().get(0).inicio());
        assertTrue(plan.salteadas().get(0).motivo().contains("09/09 21:00"),
                "el informe tiene que decir contra qué choca");
        assertEquals(6, plan.programables().size());
        assertEquals(7, funciones.listar().size());
    }

    @Test
    void laPrevisualizacionCoincideConLoQueTerminaGenerando() {
        funciones.programar(1, 1, LocalDateTime.of(2026, 9, 9, 21, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        PlanProgramacion previo = previsualizarSemana();
        PlanProgramacion alta = crearSemana(Set.of());

        assertEquals(previo.programables().stream().map(f -> f.inicio()).toList(),
                alta.programables().stream().map(f -> f.inicio()).toList());
        assertEquals(previo.salteadas().size(), alta.salteadas().size());
    }

    @Test
    void elAltaRevalidaYNoConfiaEnLaPrevisualizacion() {
        PlanProgramacion previo = previsualizarSemana();
        assertTrue(previo.salteadas().isEmpty(), "cuando se previsualizó, la sala estaba libre");

        funciones.programar(1, 1, LocalDateTime.of(2026, 9, 9, 21, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        PlanProgramacion alta = crearSemana(Set.of());

        assertEquals(1, alta.salteadas().size(), "al aplicar, la fecha ya estaba tomada");
        assertEquals(6, alta.programables().size());
    }

    @Test
    void laSegundaGrillaIgualNoGeneraNada() {
        crearSemana(Set.of());
        PlanProgramacion repetida = crearSemana(Set.of());

        assertEquals(7, repetida.salteadas().size());
        assertTrue(repetida.programables().isEmpty());
        assertEquals(7, funciones.listar().size());
        assertEquals(2, programaciones.listar().size());
    }

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

    @Test
    void previsualizarFallaIgualQueElAltaSiLaSalaNoProyectaEn3D() {
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.previsualizar(new DatosGrilla(1, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                        Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5000))));
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(new DatosGrilla(1, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                        Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5000))));
        assertTrue(programaciones.listar().isEmpty(), "la grilla inválida no se guarda");
    }

    @Test
    void rechazaUnRangoQueTerminaAntesDeEmpezar() {
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(new DatosGrilla(1, 1, DOMINGO, LUNES, LAS_2030, Set.of(),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000))));
    }

    @Test
    void rechazaPrecioCeroYPeliculaInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(new DatosGrilla(1, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(0))));
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(new DatosGrilla(99, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000))));
    }

    @Test
    void rechazaUnaGrillaQueNoCaeEnNingunDiaDelRango() {
        assertThrows(IllegalArgumentException.class,
                () -> programaciones.crear(new DatosGrilla(1, 1, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 10),
                        LAS_2030, Set.of(DayOfWeek.MONDAY), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000))));
    }

    @Test
    void unaGrillaAbiertaGeneraSoloHastaElHorizonte() {
        PlanProgramacion plan = crearAbierta();

        assertFalse(plan.funciones().isEmpty(), "una grilla abierta tiene que generar algo");
        LocalDate ultima = plan.funciones().get(plan.funciones().size() - 1).inicio().toLocalDate();
        assertFalse(ultima.isAfter(reloj.hoy().plusDays(14)),
                "no puede generar más allá del horizonte");
    }

    @Test
    void unaGrillaCerradaGeneraTodoSuRangoAunqueSeaLejano() {
        assertEquals(7, crearSemana(Set.of()).funciones().size());
    }

    @Test
    void extenderEsIdempotente() {
        crearAbierta();
        int despuesDelAlta = funcionRepository.findAll().size();

        assertEquals(0, programaciones.extenderActivas(reloj.hoy()));
        assertEquals(0, programaciones.extenderActivas(reloj.hoy()));
        assertEquals(despuesDelAlta, funcionRepository.findAll().size());
    }

    @Test
    void alPasarLosDiasExtiendeLasQueFaltan() {
        crearAbierta();
        int despuesDelAlta = funcionRepository.findAll().size();

        int generadas = programaciones.extenderActivas(reloj.hoy().plusDays(3));

        assertEquals(3, generadas, "tres días más de horizonte son tres funciones más");
        assertEquals(despuesDelAlta + 3, funcionRepository.findAll().size());
    }

    @Test
    void unaGrillaDadaDeBajaNoExtiende() {
        PlanProgramacion plan = crearAbierta();
        int despuesDelAlta = funcionRepository.findAll().size();

        programaciones.desactivar(plan.programacion().getId());

        assertEquals(0, programaciones.extenderActivas(reloj.hoy().plusDays(30)));
        assertEquals(despuesDelAlta, funcionRepository.findAll().size(),
                "las que ya generó siguen; nuevas no aparecen");
    }

    @Test
    void reactivarlaVuelveAExtender() {
        PlanProgramacion plan = crearAbierta();
        programaciones.desactivar(plan.programacion().getId());
        programaciones.extenderActivas(reloj.hoy().plusDays(10));

        programaciones.activar(plan.programacion().getId());

        assertEquals(10, programaciones.extenderActivas(reloj.hoy().plusDays(10)));
    }

    @Test
    void unaGrillaCerradaNoSePasaDeSuHasta() {
        crearSemana(Set.of());
        int delAlta = funcionRepository.findAll().size();

        assertEquals(0, programaciones.extenderActivas(DOMINGO.plusMonths(6)));
        assertEquals(delAlta, funcionRepository.findAll().size());
    }

    private void cargarGrillas() {
        crearSemana(Set.of());
        PlanProgramacion enSala2 = programaciones.crear(new DatosGrilla(1, 2, LUNES, DOMINGO, LocalTime.of(23, 0),
                Set.of(), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        programaciones.desactivar(enSala2.programacion().getId());
    }

    @Test
    void buscarSinCriteriosDevuelveTodasIncluidasLasDeBaja() {
        cargarGrillas();

        assertEquals(2, programaciones.buscar(null, null, null).size());
    }

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
        assertTrue(programaciones.buscar(null, 2, true).isEmpty());
        assertEquals(1, programaciones.buscar(null, 2, false).size());
    }

    @Test
    void buscarSinCoincidenciasDevuelveVacio() {
        cargarGrillas();

        assertTrue(programaciones.buscar(99, null, null).isEmpty());
    }

    private PlanProgramacion crearAbierta() {
        return programaciones.crear(new DatosGrilla(1, 1, reloj.hoy(), null, LAS_2030, Set.of(),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }

    private PlanProgramacion crearSemana(Set<DayOfWeek> dias) {
        return programaciones.crear(new DatosGrilla(1, 1, LUNES, DOMINGO, LAS_2030, dias,
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }

    private PlanProgramacion previsualizarSemana() {
        return programaciones.previsualizar(new DatosGrilla(1, 1, LUNES, DOMINGO, LAS_2030, Set.of(),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }
}
