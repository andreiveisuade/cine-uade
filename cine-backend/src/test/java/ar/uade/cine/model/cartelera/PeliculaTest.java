package ar.uade.cine.model.cartelera;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.Rechazo;

class PeliculaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 8, 13);

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(Rechazo.class, accion).getMessage());
    }

    private static Pelicula dune() {
        return new Pelicula("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
    }

    private static List<Genero> generos(Genero genero) {
        return genero == null ? List.of() : List.of(genero);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin título,           ,     155,        DRAMA, ATP, Falta el título
            título en blanco,     '  ', 155,        DRAMA, ATP, Falta el título
            sin duración,         Dune, ,           DRAMA, ATP, Falta la duración
            duración cero,        Dune, 0,          DRAMA, ATP, La duración tiene que estar entre 1 y 600 minutos
            duración negativa,    Dune, -5,         DRAMA, ATP, La duración tiene que estar entre 1 y 600 minutos
            más de diez horas,    Dune, 601,        DRAMA, ATP, La duración tiene que estar entre 1 y 600 minutos
            el entero más grande, Dune, 2147483647, DRAMA, ATP, La duración tiene que estar entre 1 y 600 minutos
            sin género,           Dune, 155,        ,      ATP, La película tiene que tener al menos un género
            sin clasificación,    Dune, 155,        DRAMA, ,    Falta la clasificación por edad
            todo mal: gana el título, '', 0,        ,      ,    Falta el título
            """)
    void unaPeliculaSinTituloDuracionGeneroOClasificacionNoSeConstruye(String caso, String titulo,
            Integer duracion, Genero genero, Clasificacion clasificacion, String mensaje) {
        rechaza(mensaje, () -> new Pelicula(titulo, duracion, generos(genero), clasificacion));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 600})
    void laDuracionVaDeUnMinutoADiezHoras(int minutos) {
        assertEquals(minutos, new Pelicula("Dune", minutos, List.of(Genero.DRAMA), Clasificacion.ATP)
                .getDuracionMinutos());
    }

    // Del JSON, `"generos": [null]` llega como una lista con un elemento.
    @Test
    void unGeneroNuloFalta() {
        rechaza("Falta el género", () -> new Pelicula("Dune", 155, Arrays.asList(Genero.DRAMA, null),
                Clasificacion.ATP));
    }

    @Test
    void elTituloNoPasaElLargoDeSuColumna() {
        rechaza("El título no puede tener más de 100 caracteres", () -> new Pelicula("x".repeat(101),
                155, List.of(Genero.DRAMA), Clasificacion.ATP));
    }

    @Test
    void elTituloSeGuardaSinLosEspaciosDeLosBordes() {
        Pelicula dune = new Pelicula("  Dune  ", 155, List.of(Genero.DRAMA), Clasificacion.ATP);
        assertEquals("Dune", dune.getTitulo());

        dune.actualizar(" Dune: Parte Dos ", 166, List.of(Genero.DRAMA), Clasificacion.ATP);
        assertEquals("Dune: Parte Dos", dune.getTitulo());
    }

    @Test
    void elLargoDelTituloSeMideSinLosEspaciosDeLosBordes() {
        Pelicula pelicula = new Pelicula(" " + "x".repeat(100) + " ", 155, List.of(Genero.DRAMA),
                Clasificacion.ATP);

        assertEquals(100, pelicula.getTitulo().length());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            título vacío,      '',    120, DRAMA, ATP, Falta el título
            duración cero,     Otra,  0,   DRAMA, ATP, La duración tiene que estar entre 1 y 600 minutos
            sin género,        Otra,  120, ,      ATP, La película tiene que tener al menos un género
            sin clasificación, Otra,  120, DRAMA, ,    Falta la clasificación por edad
            """)
    void actualizarPideLoMismoQueElAltaYNoTocaNadaSiRechaza(String caso, String titulo, int duracion,
            Genero genero, Clasificacion clasificacion, String mensaje) {
        Pelicula dune = dune();

        rechaza(mensaje, () -> dune.actualizar(titulo, duracion, generos(genero), clasificacion));

        assertEquals("Dune", dune.getTitulo());
        assertEquals(155, dune.getDuracionMinutos());
        assertEquals(List.of(Genero.CIENCIA_FICCION), dune.getGeneros());
        assertEquals(Clasificacion.MAS_13, dune.getClasificacion());
    }

    @Test
    void unGeneroRepetidoQuedaUnaSolaVez() {
        Pelicula matrix = new Pelicula("Matrix", 136,
                List.of(Genero.ACCION, Genero.ACCION, Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertEquals(List.of(Genero.ACCION, Genero.CIENCIA_FICCION), matrix.getGeneros());
    }

    @Test
    void cambiarElCatalogoLoReemplazaEntero() {
        Pelicula dune = dune();
        CatalogoPelicula nuevo = dune.getCatalogo()
                .conCambios("Denis Villeneuve", null, 2021, null, null, 8.1, 1200, HOY);

        dune.cambiarCatalogo(nuevo);

        assertEquals(nuevo, dune.getCatalogo());
        assertEquals(8.1, dune.getPuntaje());
        assertEquals(1200, dune.getVotos());
    }

    @Test
    void naceConfirmadaYEnCartelera() {
        Pelicula dune = dune();

        assertEquals(EstadoRevision.CONFIRMADA, dune.getEstadoRevision());
        assertTrue(dune.estaEnCartelera());
    }

    @Test
    void pendienteYDescartadaNoSeOfrecenYConfirmarLaPublica() {
        Pelicula dune = dune();

        dune.dejarPendiente();
        assertEquals(EstadoRevision.PENDIENTE, dune.getEstadoRevision());
        assertFalse(dune.estaEnCartelera());

        dune.confirmar();
        assertEquals(EstadoRevision.CONFIRMADA, dune.getEstadoRevision());
        assertTrue(dune.estaEnCartelera());

        dune.descartar();
        assertEquals(EstadoRevision.DESCARTADA, dune.getEstadoRevision());
        assertFalse(dune.estaEnCartelera());
    }

    // Cada estado de revisión decide con su propio texto (State): la descartada no pide que la revisen.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            pendiente,  PENDIENTE,  La película Dune no está confirmada: revisala antes de publicarla
            descartada, DESCARTADA, La película Dune está descartada: no se puede publicar
            """)
    void unaPendienteOUnaDescartadaNoSePublica(String caso, EstadoRevision estado, String mensaje) {
        Pelicula dune = en(estado);

        rechaza(mensaje, dune::ponerEnCartelera);

        assertFalse(dune.estaEnCartelera());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            pendiente,  PENDIENTE,  La película Dune todavía no está confirmada: revisala antes de programarla
            descartada, DESCARTADA, La película Dune está descartada: no se puede programar
            """)
    void unaPendienteOUnaDescartadaNoSeProgramaYCadaUnaDiceSuMotivo(String caso, EstadoRevision estado,
            String mensaje) {
        rechaza(mensaje, en(estado)::exigirProgramable);
    }

    @Test
    void unaConfirmadaSePublicaYSeProgramaSinRechazos() {
        Pelicula dune = dune();
        dune.sacarDeCartelera();

        assertDoesNotThrow(dune::exigirProgramable);
        dune.ponerEnCartelera();

        assertTrue(dune.estaEnCartelera());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            confirmada, CONFIRMADA, true
            pendiente,  PENDIENTE,  false
            descartada, DESCARTADA, false
            """)
    void alPublicoSoloSeLeMuestraLaConfirmada(String caso, EstadoRevision estado, boolean seMuestra) {
        assertEquals(seMuestra, en(estado).seMuestraAlPublico());
    }

    // Por las transiciones de la película, que es la que cambia de estado.
    private static Pelicula en(EstadoRevision estado) {
        Pelicula dune = dune();
        switch (estado) {
            case PENDIENTE -> dune.dejarPendiente();
            case DESCARTADA -> dune.descartar();
            case CONFIRMADA -> dune.confirmar();
        }
        return dune;
    }
}
