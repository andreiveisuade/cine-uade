package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;
import ar.uade.cine.service.salas.GestorSalas;

class PlanificadorGrillaTest extends PruebaDeIntegracion {

    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorRevisionCartelera revision;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private PlanificadorGrilla planificador;

    private Pelicula cargar(String titulo, double puntaje, Genero... generos) {
        return cargar(titulo, puntaje, 100, generos);
    }

    private Pelicula cargar(String titulo, double puntaje, int duracion, Genero... generos) {
        return cartelera.agregar(DatosPelicula.con().titulo(titulo).duracion(duracion).generos(List.of(generos))
                .clasificacion(Clasificacion.ATP).puntaje(puntaje).armar());
    }

    private Pelicula cargarEn3D(String titulo, double puntaje, int duracion) {
        return cartelera.agregar(DatosPelicula.con().titulo(titulo).duracion(duracion).generos(List.of(Genero.DRAMA))
                .clasificacion(Clasificacion.ATP).puntaje(puntaje)
                .formatos(List.of(Proyeccion.DOS_D, Proyeccion.TRES_D)).armar());
    }

    private void votar(Pelicula pelicula, int votos) {
        cartelera.editar(pelicula.getId(), DatosPelicula.con().votos(votos).armar());
    }

    private static CriteriosGrilla unDia(int cuantasPeliculas) {
        return dias(1, cuantasPeliculas);
    }

    private static CriteriosGrilla dias(int dias, int cuantasPeliculas) {
        return new CriteriosGrilla(LocalDate.of(2026, 9, 1), dias,
                LocalTime.of(14, 0), LocalTime.of(23, 0), cuantasPeliculas, Version.SUBTITULADA, Dinero.de(5000));
    }

    private static List<PaseSugerido> de(PropuestaGrilla propuesta, String sala) {
        return propuesta.pases().stream().filter(pase -> pase.sala().equals(sala)).toList();
    }

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

    @Test
    void unPuntajeAltoConPocosVotosPierdeSuVentaja() {
        Pelicula respaldada = cargar("Respaldada", 8.0, Genero.DRAMA);
        votar(respaldada, 5000);
        Pelicula flojita = cargar("Con seis votos", 9.5, Genero.SUSPENSO);
        votar(flojita, 6);
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

    @Test
    void laQueNadieVotoNoSeHundeAlFondo() {
        Pelicula votada = cargar("Votada", 7.0, Genero.DRAMA);
        votar(votada, 3000);
        Pelicula mala = cargar("Mala de verdad", 3.0, Genero.TERROR);
        votar(mala, 2000);
        cargar("Recien estrenada", 0.0, Genero.COMEDIA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        List<String> elenco = planificador.proponer(unDia(3)).elenco().stream()
                .map(Pelicula::getTitulo).toList();

        assertTrue(elenco.indexOf("Recien estrenada") < elenco.indexOf("Mala de verdad"),
                "sin votos va al promedio, y el promedio le gana a una mala con respaldo: " + elenco);
    }

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

    // Cerrar a las 00:00 es cerrar al empezar el día siguiente: la de dos horas de las 22:00 entra justa.
    @Test
    void conCierreALaMedianocheEntraElPaseQueTerminaJustoALaMedianoche() {
        cargar("Dos horas", 8.0, 120, Genero.DRAMA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));
        CriteriosGrilla deNoche = new CriteriosGrilla(LocalDate.of(2026, 9, 1), 1, LocalTime.of(22, 0),
                LocalTime.MIDNIGHT, 1, Version.SUBTITULADA, Dinero.de(5000));

        PropuestaGrilla propuesta = planificador.proponer(deNoche);

        assertEquals(List.of(LocalDateTime.of(2026, 9, 1, 22, 0)),
                propuesta.pases().stream().map(PaseSugerido::inicio).toList());
        assertEquals(120, propuesta.indicadores().minutosDisponibles(), "de 22:00 a 00:00 hay dos horas");
        assertEquals(1.0, propuesta.indicadores().ocupacion());
    }

    @Test
    void respetaLaLimpiezaEntreDosPasesDeLaMismaSala() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10), Map.of(), 20);

        PropuestaGrilla propuesta = planificador.proponer(unDia(1));
        List<PaseSugerido> pases = propuesta.pases().stream()
                .sorted(Comparator.comparing(PaseSugerido::inicio))
                .toList();

        for (int i = 1; i < pases.size(); i++) {
            LocalDateTime finAnterior = pases.get(i - 1).inicio()
                    .plusMinutes(pases.get(i - 1).duracionMinutos());
            assertFalse(pases.get(i).inicio().isBefore(finAnterior.plusMinutes(20)),
                    "el pase de las " + pases.get(i).inicio() + " pisa la limpieza");
        }
    }

    @Test
    void noPisaLasFuncionesQueYaEstabanCargadas() {
        Pelicula pelicula = cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));
        funciones.programar(pelicula.getId(), 1, LocalDateTime.of(2026, 9, 1, 16, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));

        PropuestaGrilla propuesta = planificador.proponer(unDia(1));

        for (PaseSugerido pase : propuesta.pases()) {
            LocalDateTime fin = pase.inicio().plusMinutes(pase.duracionMinutos());
            assertTrue(!funciones.agendaDe(pase.salaId(), pase.inicio(), fin).chocaEn(pase.inicio(), fin)
                    || pase.inicio().equals(LocalDateTime.of(2026, 9, 1, 16, 0)),
                    "el pase de las " + pase.inicio() + " choca con lo ya programado");
        }
    }

    @Test
    void losIndicadoresMidenOcupacionPuntajeYDiversidad() {
        cargar("Accion", 9.0, Genero.ACCION);
        cargar("Comedia", 7.0, Genero.COMEDIA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        IndicadoresGrilla indicadores = planificador.proponer(unDia(2)).indicadores();

        assertTrue(indicadores.ocupacion() > 0.5,
                "nueve horas de sala con películas de 100 minutos deberían llenarse bastante");
        assertTrue(indicadores.ocupacion() <= 1.0, "no se puede programar más tiempo del que hay");
        assertTrue(indicadores.puntajePromedio() >= 7.0 && indicadores.puntajePromedio() <= 9.0);
        assertEquals(2, indicadores.generosCubiertos());
        assertEquals(Genero.values().length, indicadores.generosTotales());
        assertTrue(indicadores.pasesPorGenero().containsKey(Genero.COMEDIA));
    }

    @Test
    void elTiempoDisponibleDescuentaLoQueYaEstabaProgramado() {
        Pelicula pelicula = cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        int sinNada = planificador.proponer(unDia(1)).indicadores().minutosDisponibles();

        funciones.programar(pelicula.getId(), 1, LocalDateTime.of(2026, 9, 1, 16, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        int conUnaCargada = planificador.proponer(unDia(1)).indicadores().minutosDisponibles();

        assertEquals(sinNada - 100, conUnaCargada,
                "los 100 minutos de la función ya cargada dejan de estar disponibles");
    }

    @Test
    void unaFuncionFueraDeLaVentanaNoDescuentaTiempo() {
        Pelicula pelicula = cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        int sinNada = planificador.proponer(unDia(1)).indicadores().minutosDisponibles();

        funciones.programar(pelicula.getId(), 1, LocalDateTime.of(2026, 9, 1, 10, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));

        assertEquals(sinNada, planificador.proponer(unDia(1)).indicadores().minutosDisponibles());
    }

    @Test
    void laOcupacionNoSePasaDeUnoNiSeVaANegativo() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        double ocupacion = planificador.proponer(unDia(1)).indicadores().ocupacion();

        assertTrue(ocupacion >= 0 && ocupacion <= 1.0, "ocupación fuera de rango: " + ocupacion);
    }

    @Test
    void aplicarCreaLasFuncionesDeLaPropuesta() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.aplicar(unDia(1));

        assertEquals(propuesta.pases().size(), funciones.listar().size());
        assertFalse(propuesta.pases().isEmpty(), "la propuesta no puede estar vacía");
    }

    @Test
    void proponerDosVecesDaLaMismaGrilla() {
        cargar("Accion", 9.0, Genero.ACCION);
        cargar("Comedia", 7.0, Genero.COMEDIA);
        cargar("Drama", 8.0, Genero.DRAMA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        assertEquals(planificador.proponer(unDia(2)).pases(),
                planificador.proponer(unDia(2)).pases());
    }

    @Test
    void sinPeliculasConfirmadasAvisaQueRevisenElBuzon() {
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        Rechazo e = assertThrows(Rechazo.class,
                () -> planificador.proponer(unDia(3)));

        assertTrue(e.getMessage().contains("buzón"), e.getMessage());
    }

    // Ninguna sala queda vacía ni con un hueco donde entraría otra película del elenco.
    @Test
    void cadaSalaQuedaLlenaHastaQueNoEntraNingunaMas() {
        cargarEn3D("Larga en 3D", 8.0, 150);
        cargar("Media", 9.0, 120, Genero.ACCION);
        cargar("Corta", 7.0, 90, Genero.COMEDIA);
        salas.agregar("Sala 4D", TipoSala.CUATRO_D, List.of(10));
        salas.agregar("Sala IMAX", TipoSala.IMAX, List.of(10));
        salas.agregar("Sala 3D", TipoSala.TRES_D, List.of(10));
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(3));

        LocalDateTime cierre = LocalDateTime.of(2026, 9, 1, 23, 0);
        for (String sala : List.of("Sala 4D", "Sala IMAX", "Sala 3D", "Sala 2D")) {
            PaseSugerido ultimo = de(propuesta, sala).stream().max(Comparator.comparing(PaseSugerido::inicio))
                    .orElseThrow(() -> new AssertionError(sala + " quedó vacía"));
            LocalDateTime proximo = ultimo.inicio().plusMinutes(ultimo.duracionMinutos() + 15L);
            assertTrue(proximo.plusMinutes(90).isAfter(cierre), sala + " tiene lugar para otra a las " + proximo);
        }
    }

    // Desigualdad del reordenamiento: la sala más cara con las mejor puntuadas, y la mejor de todas primero.
    @Test
    void laSalaPremiumDaLasMejorPuntuadas() {
        cargar("Excelente", 9.0, Genero.ACCION);
        cargar("Muy buena", 8.0, Genero.COMEDIA);
        cargar("Floja", 5.0, Genero.DRAMA);
        cargar("Muy floja", 4.0, Genero.TERROR);
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(10));
        salas.agregar("Sala IMAX", TipoSala.IMAX, List.of(10));

        List<PaseSugerido> enLaImax = de(planificador.proponer(unDia(4)), "Sala IMAX");

        assertEquals("Excelente", enLaImax.get(0).titulo());
        assertEquals(Set.of("Excelente", "Muy buena"),
                enLaImax.stream().map(PaseSugerido::titulo).collect(Collectors.toSet()));
    }

    @Test
    void laSala3DDaLasVersiones3DEn3D() {
        cargarEn3D("Con 3D", 7.0, 100);
        cargar("Plana buena", 9.0, Genero.ACCION);
        cargar("Plana", 8.0, Genero.COMEDIA);
        salas.agregar("Sala 3D", TipoSala.TRES_D, List.of(10));
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(3));

        assertFalse(de(propuesta, "Sala 3D").isEmpty());
        assertTrue(de(propuesta, "Sala 3D").stream()
                .allMatch(pase -> pase.titulo().equals("Con 3D") && pase.proyeccion() == Proyeccion.TRES_D));
        assertTrue(de(propuesta, "Sala 2D").stream().allMatch(pase -> pase.proyeccion() == Proyeccion.DOS_D));
    }

    // Una sala 3D también proyecta 2D: sin versiones 3D en el elenco no queda vacía.
    @Test
    void sinVersiones3DLaSala3DDa2D() {
        cargar("Plana", 8.0, Genero.COMEDIA);
        salas.agregar("Sala 3D", TipoSala.TRES_D, List.of(10));

        List<PaseSugerido> pases = planificador.proponer(unDia(1)).pases();

        assertFalse(pases.isEmpty());
        assertTrue(pases.stream().allMatch(pase -> pase.proyeccion() == Proyeccion.DOS_D));
    }

    // La IMAX da las dos mejores; la 2D, que va última, arranca por las que todavía no tienen pase.
    @Test
    void ningunaPeliculaQuedaSinPaseEnElDiaSiEntra() {
        cargar("Excelente", 9.0, Genero.ACCION);
        cargar("Muy buena", 8.0, Genero.COMEDIA);
        cargar("Floja", 5.0, Genero.DRAMA);
        cargar("Muy floja", 4.0, Genero.TERROR);
        salas.agregar("Sala IMAX", TipoSala.IMAX, List.of(10));
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(dias(2, 4));

        for (LocalDate dia : List.of(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2))) {
            Set<String> conPase = propuesta.pases().stream()
                    .filter(pase -> pase.inicio().toLocalDate().equals(dia))
                    .map(PaseSugerido::titulo).collect(Collectors.toSet());
            assertEquals(Set.of("Excelente", "Muy buena", "Floja", "Muy floja"), conPase, dia.toString());
        }
    }

    // R8 y R31 otra vez al aplicar: cada función nace en el formato del pase que la propuso.
    @Test
    void aplicarCreaCadaFuncionEnElFormatoDeSuPase() {
        cargarEn3D("Con 3D", 8.0, 100);
        salas.agregar("Sala 3D", TipoSala.TRES_D, List.of(10));
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.aplicar(unDia(1));

        assertEquals(propuesta.pases().stream().map(PaseSugerido::proyeccion).sorted().toList(),
                funciones.listar().stream().map(Funcion::getProyeccion).sorted().toList());
        assertTrue(funciones.listar().stream().anyMatch(f -> f.getProyeccion() == Proyeccion.TRES_D));
    }

    // A las 22:00 la Larga tiene más deuda pero terminaría a las 00:30: se descarta y entra la Corta.
    @Test
    void siLaDeMasDeudaNoEntraAntesDelCierreVaLaSiguiente() {
        cargar("Larga", 9.0, 150, Genero.DRAMA);
        cargar("Corta", 8.0, 60, Genero.COMEDIA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(2));

        assertEquals(List.of("14:00 Larga", "16:45 Corta", "18:00 Larga", "20:45 Corta", "22:00 Corta"),
                propuesta.pases().stream().map(p -> p.inicio().toLocalTime() + " " + p.titulo()).toList());
    }

    // A las 16:45 quedan 135 minutos: la Media dejaría 35 vacíos; la Corta deja lugar para otra Corta justa.
    @Test
    void enElUltimoTurnoVaLaQueMenosHuecoDejaAlCierre() {
        cargar("Larga", 9.0, 150, Genero.DRAMA);
        cargar("Media", 8.0, 100, Genero.ACCION);
        cargar("Corta", 7.0, 60, Genero.COMEDIA);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));
        CriteriosGrilla deTarde = new CriteriosGrilla(LocalDate.of(2026, 9, 1), 1, LocalTime.of(14, 0),
                LocalTime.of(19, 0), 3, Version.SUBTITULADA, Dinero.de(5000));

        PropuestaGrilla propuesta = planificador.proponer(deTarde);

        assertEquals(List.of("14:00 Larga", "16:45 Corta", "18:00 Corta"),
                propuesta.pases().stream().map(p -> p.inicio().toLocalTime() + " " + p.titulo()).toList());
    }

    @Test
    void laOcupacionSeMideSobreTodasLasSalas() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(10));
        salas.agregar("Sala IMAX", TipoSala.IMAX, List.of(10));

        IndicadoresGrilla indicadores = planificador.proponer(unDia(1)).indicadores();

        assertEquals(2 * 540, indicadores.minutosDisponibles(), "de 14 a 23 en las dos salas");
    }

    // Diez butacas estándar a $ 5000 en 2D son $ 50.000 a sala llena; con puntaje 8, se espera el 80 %.
    @Test
    void laFacturacionEsperadaEsLaSalaLlenaPorElPuntaje() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(10));

        PropuestaGrilla propuesta = planificador.proponer(unDia(1));

        assertEquals(propuesta.pases().size() * 40_000.0,
                propuesta.indicadores().facturacionEsperada().aPesos());
    }

    @Test
    void sinSalasNoHayGrillaPosible() {
        cargar("Una", 8.0, Genero.ACCION);

        rechaza("No hay salas cargadas para programar", () -> planificador.proponer(unDia(1)));
    }

    // R20: a las 15:10 de hoy, el primer intento libre es el de las 15:30 (de 8:00, cada 30).
    @Test
    void hoyNoProponePasesQueYaPasaron() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));
        reloj.mover(reloj.hoy().atTime(15, 10));
        CriteriosGrilla hoy = new CriteriosGrilla(reloj.hoy(), 2, LocalTime.of(8, 0), LocalTime.of(23, 0),
                1, Version.SUBTITULADA, Dinero.de(5000));

        PropuestaGrilla propuesta = planificador.aplicar(hoy);

        assertFalse(propuesta.pases().isEmpty());
        assertEquals(reloj.hoy().atTime(15, 30), propuesta.pases().get(0).inicio());
        assertTrue(propuesta.pases().stream().allMatch(p -> p.inicio().isAfter(reloj.ahora())));
        assertEquals(propuesta.pases().size(), funciones.listar().size(), "aplicar no choca con R20");
    }

    // R20: a las 15:10 la ventana de hoy va de 15:30 a 23:00 (450 min); la de las 20:00 descuenta sus 100.
    @Test
    void hoyLosMinutosDisponiblesSonSoloLosQueNoPasaron() {
        cargar("Una", 8.0, Genero.ACCION);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10));
        funciones.programar(1, 1, reloj.hoy().atTime(14, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        funciones.programar(1, 1, reloj.hoy().atTime(20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        reloj.mover(reloj.hoy().atTime(15, 10));
        CriteriosGrilla hoy = new CriteriosGrilla(reloj.hoy(), 1, LocalTime.of(14, 0), LocalTime.of(23, 0),
                1, Version.SUBTITULADA, Dinero.de(5000));

        IndicadoresGrilla indicadores = planificador.proponer(hoy).indicadores();

        assertEquals(350, indicadores.minutosDisponibles());
        assertEquals((double) indicadores.minutosProgramados() / 350, indicadores.ocupacion());
        assertTrue(indicadores.ocupacion() <= 1.0, "no se programa más de lo que queda del día");
    }
}
