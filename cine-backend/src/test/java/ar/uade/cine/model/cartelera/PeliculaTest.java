package ar.uade.cine.model.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PeliculaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 8, 13);

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, accion).getMessage());
    }

    private static Pelicula dune() {
        return new Pelicula("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
    }

    private static List<Genero> generos(Genero genero) {
        return genero == null ? List.of() : List.of(genero);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin título,           ,     155, DRAMA, ATP, El título no puede estar vacío
            título en blanco,     '  ', 155, DRAMA, ATP, El título no puede estar vacío
            duración cero,        Dune, 0,   DRAMA, ATP, La duración tiene que ser mayor a cero
            duración negativa,    Dune, -5,  DRAMA, ATP, La duración tiene que ser mayor a cero
            sin género,           Dune, 155, ,      ATP, La película necesita al menos un género
            sin clasificación,    Dune, 155, DRAMA, ,    Falta la clasificación por edad
            todo mal: gana el título, '', 0, ,      ,    El título no puede estar vacío
            """)
    void unaPeliculaSinTituloDuracionGeneroOClasificacionNoSeConstruye(String caso, String titulo,
            int duracion, Genero genero, Clasificacion clasificacion, String mensaje) {
        rechaza(mensaje, () -> new Pelicula(titulo, duracion, generos(genero), clasificacion));
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
            título vacío,      '',    120, DRAMA, ATP, El título no puede estar vacío
            duración cero,     Otra,  0,   DRAMA, ATP, La duración tiene que ser mayor a cero
            sin género,        Otra,  120, ,      ATP, La película necesita al menos un género
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
    void elPuntajeVaDeCeroADiezYLosVotosNoSonNegativos() {
        Pelicula dune = dune();

        rechaza("El puntaje tiene que estar entre 0 y 10", () -> dune.cambiarPuntaje(-0.1));
        rechaza("El puntaje tiene que estar entre 0 y 10", () -> dune.cambiarPuntaje(10.1));
        rechaza("Los votos no pueden ser negativos", () -> dune.cambiarVotos(-1));
        dune.cambiarPuntaje(10);
        dune.cambiarVotos(0);

        assertEquals(10, dune.getPuntaje());
        assertEquals(0, dune.getVotos());
    }

    @Test
    void losTextosDelCatalogoNoPasanElLargoDeSuColumnaYSiRechazaNoLosToca() {
        Pelicula dune = dune();

        rechaza("El director no puede tener más de 100 caracteres",
                () -> dune.cambiarDirector("x".repeat(101)));
        rechaza("El idioma original no puede tener más de 40 caracteres",
                () -> dune.cambiarIdiomaOriginal("x".repeat(41)));
        rechaza("La URL del póster no puede tener más de 255 caracteres",
                () -> dune.cambiarPoster("x".repeat(256)));

        assertEquals("", dune.getDirector());
        assertEquals("", dune.getIdiomaOriginal());
        assertEquals("", dune.getPosterUrl());
    }

    @ParameterizedTest
    @ValueSource(ints = {-3, 1894, 2032})
    void elAnioVaDelPrimerCineACincoAniosPorDelante(int anio) {
        rechaza("El año tiene que estar entre 1895 y 2031", () -> dune().cambiarAnio(anio, HOY));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1895, 2031})
    void ceroEsSinDatoYLosBordesValen(int anio) {
        Pelicula dune = dune();

        dune.cambiarAnio(anio, HOY);

        assertEquals(anio, dune.getAnio());
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

    @Test
    void soloLaConfirmadaEstaConfirmada() {
        Pelicula dune = dune();
        assertTrue(dune.estaConfirmada());

        dune.dejarPendiente();
        assertFalse(dune.estaConfirmada());

        dune.descartar();
        assertFalse(dune.estaConfirmada());
    }

    // El botón Publicar no puede saltear el buzón: publicar es cosa de confirmar.
    @Test
    void unaPendienteOUnaDescartadaNoSePublica() {
        Pelicula pendiente = dune();
        pendiente.dejarPendiente();
        Pelicula descartada = dune();
        descartada.descartar();

        rechaza("La película Dune no está confirmada: revisala antes de publicarla",
                pendiente::ponerEnCartelera);
        rechaza("La película Dune no está confirmada: revisala antes de publicarla",
                descartada::ponerEnCartelera);

        assertFalse(pendiente.estaEnCartelera());
        assertFalse(descartada.estaEnCartelera());
    }
}
