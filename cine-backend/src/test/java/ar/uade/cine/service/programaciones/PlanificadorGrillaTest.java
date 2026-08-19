package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.repository.AsientoDAO;
import ar.uade.cine.repository.FuncionDAO;
import ar.uade.cine.repository.PeliculaDAO;
import ar.uade.cine.repository.SalaDAO;
import ar.uade.cine.repository.memoria.AsientoDAOMemoria;
import ar.uade.cine.repository.memoria.FuncionDAOMemoria;
import ar.uade.cine.repository.memoria.PeliculaDAOMemoria;
import ar.uade.cine.repository.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.repository.memoria.ReservaDAOMemoria;
import ar.uade.cine.repository.memoria.SalaDAOMemoria;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;

/**
 * El planificador optimiza tres cosas a la vez —puntaje, diversidad y ocupación— y cada
 * una se prueba por separado, porque el riesgo es justamente que una se coma a las otras.
 */
class PlanificadorGrillaTest {

    private final PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
    private final SalaDAO salaDAO = new SalaDAOMemoria();
    private final AsientoDAO asientoDAO = new AsientoDAOMemoria();
    private final FuncionDAO funcionDAO = new FuncionDAOMemoria();

    private GestorCartelera cartelera;
    private GestorRevisionCartelera revision;
    private GestorSalas salas;
    private GestorFunciones funciones;
    private PlanificadorGrilla planificador;

    @BeforeEach
    void prepararCine() {
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, new ReservaDAOMemoria());
        cartelera = new GestorCartelera(peliculaDAO, funcionDAO, new GestorProgramaciones(
                new ProgramacionDAOMemoria(), funcionDAO, funciones));
        revision = new GestorRevisionCartelera(peliculaDAO, funcionDAO, cartelera);
        salas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        planificador = new PlanificadorGrilla(peliculaDAO, salaDAO, funciones);
    }

    /** Una película de 100 minutos, con el puntaje y los géneros que pida el caso. */
    private Pelicula cargar(String titulo, double puntaje, Genero... generos) {
        return cargar(titulo, puntaje, 100, generos);
    }

    private Pelicula cargar(String titulo, double puntaje, int duracion, Genero... generos) {
        Pelicula pelicula = cartelera.agregar(titulo, duracion, List.of(generos), Clasificacion.ATP);
        pelicula.setPuntaje(puntaje);
        cartelera.actualizar(pelicula);
        return pelicula;
    }

    private CriteriosGrilla unDia(int cuantasPeliculas) {
        return new CriteriosGrilla(LocalDate.of(2026, 9, 1), 1,
                LocalTime.of(14, 0), LocalTime.of(23, 0), cuantasPeliculas, Dinero.de(5000),
                Version.SUBTITULADA, Proyeccion.DOS_D);
    }

    // ---------- etapa 1: a quiénes elige ----------

    @Test
    void aIgualdadDeGenerosEligeLasMejorPuntuadas() {
        cargar("Buena", 9.0, Genero.ACCION);
        cargar("Regular", 5.0, Genero.ACCION);
        cargar("Mala", 2.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(2));

        assertEquals(List.of("Buena", "Regular"),
                propuesta.elenco().stream().map(Pelicula::getTitulo).toList());
    }

    /**
     * El caso que justifica todo el algoritmo: la cuarta película de acción está mejor
     * puntuada que la única comedia, y aun así entra la comedia. Sin el bono por género
     * nuevo, la grilla de la semana sería acción todo el día.
     */
    @Test
    void prefiereUnGeneroNuevoAntesQueLaCuartaDelMismoGenero() {
        cargar("Accion 1", 9.0, Genero.ACCION);
        cargar("Accion 2", 8.6, Genero.ACCION);
        cargar("Accion 3", 8.5, Genero.ACCION);
        cargar("La comedia", 7.0, Genero.COMEDIA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(2));

        List<String> titulos = propuesta.elenco().stream().map(Pelicula::getTitulo).toList();
        assertEquals("Accion 1", titulos.get(0), "la primera elección es la mejor a secas");
        assertEquals("La comedia", titulos.get(1),
                "la segunda tiene que ganarse el lugar contra lo que ya hay");
    }

    /** Una diferencia de puntaje grande sí le gana al bono: el bono inclina, no decide. */
    @Test
    void unPuntajeMuchoMejorLeGanaAlBonoPorGeneroNuevo() {
        cargar("Accion 1", 9.5, Genero.ACCION);
        cargar("Accion 2", 9.4, Genero.ACCION);
        cargar("Documental flojo", 4.0, Genero.DOCUMENTAL);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(2));

        assertEquals(List.of("Accion 1", "Accion 2"),
                propuesta.elenco().stream().map(Pelicula::getTitulo).toList());
    }

    /**
     * El caso que aparece con datos de TMDB y no con películas de test: los géneros de ahí
     * son generosos, y una película figura a la vez como acción, animación, ciencia ficción
     * y comedia. Con un bono lineal esas cuatro etiquetas valían ocho puntos y le ganaban a
     * la mejor del catálogo por estar mejor catalogada, no por ser mejor ni por aportar
     * cuatro veces más variedad.
     */
    @Test
    void muchosGenerosNoLeGananALaMejorPelicula() {
        cargar("La mejor", 9.2, Genero.DRAMA, Genero.SUSPENSO);
        cargar("La etiquetada", 7.2,
                Genero.ACCION, Genero.ANIMACION, Genero.CIENCIA_FICCION, Genero.COMEDIA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(2));

        assertEquals("La mejor", propuesta.elenco().get(0).getTitulo(),
                "la primera elección es la mejor película a secas, sin bono de por medio");
        assertEquals("La etiquetada", propuesta.elenco().get(1).getTitulo(),
                "y en la segunda vuelta sí pesa la variedad que agrega");
    }

    /**
     * El bono crece cada vez menos, pero sigue creciendo: entre dos películas de igual
     * puntaje, la que aporta más géneros nuevos entra antes.
     */
    @Test
    void aIgualPuntajeEntraLaQueAportaMasGeneros() {
        cargar("Ancla", 9.5, Genero.DRAMA);
        cargar("Aporta uno", 7.0, Genero.TERROR);
        cargar("Aporta tres", 7.0, Genero.ACCION, Genero.COMEDIA, Genero.ROMANCE);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(2));

        assertEquals("Ancla", propuesta.elenco().get(0).getTitulo());
        assertEquals("Aporta tres", propuesta.elenco().get(1).getTitulo());
    }

    /**
     * Seis votos no pesan como cinco mil, y el reparto de pases lo tiene que mostrar.
     *
     * <p>La afirmación no es que la respaldada gane —un 9,5 puede ser genuino y el sistema
     * no tiene cómo saberlo—, sino que la <strong>ventaja se achica</strong>: sin corregir,
     * la diferencia de puntaje es de 1,5 puntos y se traduce en muchos más pases; corregida,
     * las dos quedan a la par, que es lo honesto cuando de una hay seis opiniones.
     */
    @Test
    void unPuntajeAltoConPocosVotosPierdeSuVentaja() {
        Pelicula respaldada = cargar("Respaldada", 8.0, Genero.DRAMA);
        respaldada.setVotos(5000);
        cartelera.actualizar(respaldada);
        Pelicula flojita = cargar("Con seis votos", 9.5, Genero.SUSPENSO);
        flojita.setVotos(6);
        cartelera.actualizar(flojita);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(2));

        long deLaRespaldada = propuesta.pases().stream()
                .filter(p -> p.titulo().equals("Respaldada")).count();
        long deLaFlojita = propuesta.pases().stream()
                .filter(p -> p.titulo().equals("Con seis votos")).count();

        assertTrue(Math.abs(deLaRespaldada - deLaFlojita) <= 1,
                "con los votos corregidos tienen que repartirse casi igual, y quedaron "
                        + deLaRespaldada + " contra " + deLaFlojita);
    }

    /**
     * El 0,0 de TMDB no es una nota mala sino una película que nadie vio todavía: siete de
     * cada veinte de la cartelera argentina están así. Sin esto quedaban últimas siempre.
     */
    @Test
    void laQueNadieVotoNoSeHundeAlFondo() {
        Pelicula votada = cargar("Votada", 7.0, Genero.DRAMA);
        votada.setVotos(3000);
        cartelera.actualizar(votada);
        Pelicula mala = cargar("Mala de verdad", 3.0, Genero.TERROR);
        mala.setVotos(2000);
        cartelera.actualizar(mala);
        cargar("Recien estrenada", 0.0, Genero.COMEDIA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        List<String> elenco = planificador.proponer(unDia(3)).elenco().stream()
                .map(Pelicula::getTitulo).toList();

        assertTrue(elenco.indexOf("Recien estrenada") < elenco.indexOf("Mala de verdad"),
                "sin votos va al promedio, y el promedio le gana a una mala con respaldo: " + elenco);
    }

    /**
     * El puntaje que carga el encargado a mano no es un promedio flojo: es su criterio. Sin
     * esta distinción, un catálogo entero sin votos quedaba con todas las películas valiendo
     * lo mismo y el ranking desaparecía.
     */
    @Test
    void elPuntajeCargadoAManoOrdenaAunqueNoHayaVotos() {
        cargar("Buena", 9.0, Genero.ACCION);
        cargar("Regular", 5.0, Genero.DRAMA);
        cargar("Floja", 2.0, Genero.TERROR);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(2));

        assertEquals("Buena", propuesta.elenco().get(0).getTitulo());
    }

    @Test
    void noEligeLoQueTodaviaEstaEnElBuzon() {
        cargar("Confirmada", 6.0, Genero.DRAMA);
        revision.importar(DatosPelicula.deAlta("Importada", 100, List.of(Genero.ACCION), Clasificacion.ATP));
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(5));

        assertEquals(List.of("Confirmada"),
                propuesta.elenco().stream().map(Pelicula::getTitulo).toList());
    }

    // ---------- etapa 2: cómo las reparte ----------

    /** El reparto es proporcional al puntaje, no en partes iguales. */
    @Test
    void laMejorPuntuadaSeLlevaMasPases() {
        cargar("Muy buena", 9.0, Genero.ACCION);
        cargar("Floja", 3.0, Genero.COMEDIA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(2));

        long deLaBuena = propuesta.pases().stream().filter(p -> p.titulo().equals("Muy buena")).count();
        long deLaFloja = propuesta.pases().stream().filter(p -> p.titulo().equals("Floja")).count();
        assertTrue(deLaBuena > deLaFloja,
                "la mejor puntuada tiene que ir más veces: " + deLaBuena + " vs " + deLaFloja);
    }

    /** Ningún pase puede terminar después del cierre, aunque empiece antes. */
    @Test
    void ningunPaseSePasaDelHorarioDeCierre() {
        cargar("Larga", 8.0, 180, Genero.DRAMA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(1));

        LocalDateTime cierre = LocalDateTime.of(2026, 9, 1, 23, 0);
        for (PaseSugerido pase : propuesta.pases()) {
            assertFalse(pase.inicio().plusMinutes(pase.duracionMinutos()).isAfter(cierre),
                    "el pase de las " + pase.inicio() + " termina después de cerrar");
        }
    }

    /** Entre dos pases de la misma sala tiene que entrar la limpieza (R3). */
    @Test
    void respetaLaLimpiezaEntreDosPasesDeLaMismaSala() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10), Map.of(), 20);

        PropuestaGrilla propuesta = planificador.proponer(unDia(1));
        List<PaseSugerido> pases = propuesta.pases().stream()
                .sorted(java.util.Comparator.comparing(PaseSugerido::inicio))
                .toList();

        for (int i = 1; i < pases.size(); i++) {
            LocalDateTime finAnterior = pases.get(i - 1).inicio()
                    .plusMinutes(pases.get(i - 1).duracionMinutos());
            assertFalse(pases.get(i).inicio().isBefore(finAnterior.plusMinutes(20)),
                    "el pase de las " + pases.get(i).inicio() + " pisa la limpieza");
        }
    }

    /**
     * No reescribe R3: si la sala ya tiene una función cargada a mano, la propuesta la
     * respeta en vez de pisarla.
     */
    @Test
    void noPisaLasFuncionesQueYaEstabanCargadas() {
        Pelicula pelicula = cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));
        funciones.programar(pelicula.getId(), 1, LocalDateTime.of(2026, 9, 1, 16, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        PropuestaGrilla propuesta = planificador.proponer(unDia(1));

        for (PaseSugerido pase : propuesta.pases()) {
            assertTrue(funciones.superpuestaEn(pase.salaId(), pase.inicio(),
                    pase.inicio().plusMinutes(pase.duracionMinutos())).isEmpty()
                    || pase.inicio().equals(LocalDateTime.of(2026, 9, 1, 16, 0)),
                    "el pase de las " + pase.inicio() + " choca con lo ya programado");
        }
    }

    // ---------- los indicadores ----------

    @Test
    void losIndicadoresMidenOcupacionPuntajeYDiversidad() {
        cargar("Accion", 9.0, Genero.ACCION);
        cargar("Comedia", 7.0, Genero.COMEDIA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla.IndicadoresGrilla indicadores = planificador.proponer(unDia(2)).indicadores();

        assertTrue(indicadores.ocupacion() > 0.5,
                "nueve horas de sala con películas de 100 minutos deberían llenarse bastante");
        assertTrue(indicadores.ocupacion() <= 1.0, "no se puede programar más tiempo del que hay");
        assertTrue(indicadores.puntajePromedio() >= 7.0 && indicadores.puntajePromedio() <= 9.0);
        assertEquals(2, indicadores.generosCubiertos());
        assertEquals(Genero.values().length, indicadores.generosTotales());
        assertTrue(indicadores.pasesPorGenero().containsKey(Genero.COMEDIA));
    }

    /**
     * El caso que apareció con la base cargada: una semana que ya tiene funciones daba una
     * ocupación baja —la propuesta apenas encontraba huecos— y se leía como que el cine
     * estaba vacío, cuando era exactamente al revés. El tiempo que la propuesta podía usar
     * es la ventana menos lo ya programado, no la ventana entera.
     */
    @Test
    void elTiempoDisponibleDescuentaLoQueYaEstabaProgramado() {
        Pelicula pelicula = cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        int sinNada = planificador.proponer(unDia(1)).indicadores().minutosDisponibles();

        funciones.programar(pelicula.getId(), 1, LocalDateTime.of(2026, 9, 1, 16, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        int conUnaCargada = planificador.proponer(unDia(1)).indicadores().minutosDisponibles();

        assertEquals(sinNada - 100, conUnaCargada,
                "los 100 minutos de la función ya cargada dejan de estar disponibles");
    }

    /** Una función fuera de la ventana ocupa la sala, pero no le saca lugar a la grilla. */
    @Test
    void unaFuncionFueraDeLaVentanaNoDescuentaTiempo() {
        Pelicula pelicula = cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        int sinNada = planificador.proponer(unDia(1)).indicadores().minutosDisponibles();

        // La ventana del test es de 14 a 23: esta función de la mañana queda afuera.
        funciones.programar(pelicula.getId(), 1, LocalDateTime.of(2026, 9, 1, 10, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        assertEquals(sinNada, planificador.proponer(unDia(1)).indicadores().minutosDisponibles());
    }

    /** La ocupación nunca puede pasar de 1: es lo que vuelve comparable el número. */
    @Test
    void laOcupacionNoSePasaDeUnoNiSeVaANegativo() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        double ocupacion = planificador.proponer(unDia(1)).indicadores().ocupacion();

        assertTrue(ocupacion >= 0 && ocupacion <= 1.0, "ocupación fuera de rango: " + ocupacion);
    }

    // ---------- aplicar ----------

    @Test
    void aplicarCreaLasFuncionesDeLaPropuesta() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.aplicar(unDia(1));

        assertEquals(propuesta.pases().size(), funciones.listar().size());
        assertFalse(propuesta.pases().isEmpty(), "la propuesta no puede estar vacía");
    }

    /** Determinista: dos corridas con los mismos criterios dan lo mismo. */
    @Test
    void proponerDosVecesDaLaMismaGrilla() {
        cargar("Accion", 9.0, Genero.ACCION);
        cargar("Comedia", 7.0, Genero.COMEDIA);
        cargar("Drama", 8.0, Genero.DRAMA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        assertEquals(planificador.proponer(unDia(2)).pases(),
                planificador.proponer(unDia(2)).pases());
    }

    // ---------- lo que rechaza ----------

    @Test
    void sinPeliculasConfirmadasAvisaQueRevisenElBuzon() {
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> planificador.proponer(unDia(3)));

        assertTrue(e.getMessage().contains("buzón"), e.getMessage());
    }

    @Test
    void sinSalasNoHayGrillaPosible() {
        cargar("Una", 8.0, Genero.ACCION);

        assertThrows(IllegalArgumentException.class, () -> planificador.proponer(unDia(1)));
    }

    @Test
    void rechazaUnaVentanaHorariaImposible() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        CriteriosGrilla alReves = new CriteriosGrilla(LocalDate.of(2026, 9, 1), 1,
                LocalTime.of(23, 0), LocalTime.of(14, 0), 2, Dinero.de(5000),
                Version.SUBTITULADA, Proyeccion.DOS_D);

        assertThrows(IllegalArgumentException.class, () -> planificador.proponer(alReves));
    }
}
