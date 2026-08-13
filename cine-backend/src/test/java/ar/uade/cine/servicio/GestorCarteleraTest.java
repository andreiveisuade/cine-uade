package ar.uade.cine.servicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.cartelera.PeliculaImpl;
import ar.uade.cine.dominio.funciones.FuncionImpl;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;

/**
 * Gracias a que GestorCartelera depende de la interfaz, se puede testear la lógica
 * con el DAO en memoria: los tests corren sin MySQL levantado.
 */
class GestorCarteleraTest {

    // El DAO de funciones queda accesible porque estar en cartelera se deriva de tener
    // funciones por delante: sin poder programarlas, no se puede probar la cartelera.
    private final FuncionDAO funcionDAO = new FuncionDAOMemoria();
    private final GestorCartelera gestor = new GestorCartelera(new PeliculaDAOMemoria(), funcionDAO);

    /** Una función de esa película dentro de una semana, que es lo que la pone en cartelera. */
    private void programarProxima(int peliculaId) {
        funcionDAO.guardar(new FuncionImpl(peliculaId, 1, LocalDateTime.now().plusDays(7),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000));
    }

    private void programarPasada(int peliculaId) {
        funcionDAO.guardar(new FuncionImpl(peliculaId, 1, LocalDateTime.now().minusDays(1),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000));
    }

    @Test
    void agregaYLista() {
        gestor.agregar("El Padrino", 175, List.of(Genero.DRAMA), Clasificacion.ATP);
        assertEquals(1, gestor.listar().size());
        assertTrue(gestor.buscar(1).isPresent());
    }

    @Test
    void rechazaTituloRepetido() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP));
    }

    @Test
    void rechazaDuracionInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("Sin duración", 0, List.of(Genero.DRAMA), Clasificacion.ATP));
    }

    @Test
    void rechazaPeliculaSinGenero() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("Sin género", 100, List.of(), Clasificacion.ATP));
    }

    @Test
    void guardaLaClasificacionYSuEtiqueta() {
        gestor.agregar("Terrifier", 100, List.of(Genero.TERROR), Clasificacion.MAS_18);

        Pelicula pelicula = gestor.buscar(1).orElseThrow();
        assertEquals(Clasificacion.MAS_18, pelicula.getClasificacion());
        assertEquals(18, pelicula.getClasificacion().getEdadMinima());
        assertEquals("+18", pelicula.getClasificacion().getEtiqueta());
        assertEquals("ATP", Clasificacion.ATP.getEtiqueta());
    }

    @Test
    void rechazaPeliculaSinClasificacion() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("Sin clasificar", 100, List.of(Genero.DRAMA), null));
    }

    @Test
    void losDatosDeCatalogoSeCarganDespuesDelAlta() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        pelicula.setDirector("Denis Villeneuve");
        pelicula.setAnio(2021);
        pelicula.setIdiomaOriginal("Inglés");
        gestor.actualizar(pelicula);

        Pelicula leida = gestor.buscar(pelicula.getId()).orElseThrow();
        assertEquals("Denis Villeneuve", leida.getDirector());
        assertEquals(2021, leida.getAnio());
    }

    // ---------- la cartelera se deriva de la programación ----------

    /** El flag sigue pudiendo bajar una película, aunque tenga funciones programadas. */
    @Test
    void laCarteleraExcluyeLasPeliculasDadasDeBaja() {
        Pelicula vieja = gestor.agregar("Titanic", 194, List.of(Genero.ROMANCE), Clasificacion.MAS_13);
        Pelicula actual = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarProxima(vieja.getId());
        programarProxima(actual.getId());

        vieja.setEnCartelera(false);
        gestor.actualizar(vieja);

        assertEquals(2, gestor.listar().size());
        assertEquals(1, gestor.listarEnCartelera().size());
        assertEquals("Dune", gestor.listarEnCartelera().get(0).getTitulo());
    }

    /**
     * Lo que antes había que declarar a mano: una película cargada pero sin funciones no
     * está en cartelera, por más que el flag venga en true al darla de alta.
     */
    @Test
    void unaPeliculaSinFuncionesNoEstaEnCartelera() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertTrue(pelicula.estaEnCartelera(), "el flag arranca en true");
        assertEquals(0, gestor.listarEnCartelera().size(), "pero sin funciones no está en cartelera");
    }

    @Test
    void programarUnaFuncionLaPoneEnCartelera() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarProxima(pelicula.getId());

        assertEquals(1, gestor.listarEnCartelera().size());
    }

    /** El caso que el flag manual nunca resolvía: la última función ya pasó. */
    @Test
    void saleDeCarteleraSolaCuandoSusFuncionesQuedaronAtras() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarPasada(pelicula.getId());

        assertTrue(pelicula.estaEnCartelera(), "nadie tocó el flag");
        assertEquals(0, gestor.listarEnCartelera().size(), "y aun así ya no está en cartelera");
    }

    @Test
    void conUnaFuncionPasadaYOtraFuturaSigueEnCartelera() {
        Pelicula pelicula = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        programarPasada(pelicula.getId());
        programarProxima(pelicula.getId());

        assertEquals(1, gestor.listarEnCartelera().size());
    }

    /**
     * R1 al editar. Hoy Pelicula no deja cambiar el título, pero quien reconstruya una
     * película con un id existente —un adaptador HTTP, por ejemplo— sí podría duplicarlo.
     */
    @Test
    void noSePuedeEditarUnaPeliculaParaQueQuedeConElTituloDeOtra() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        Pelicula renombrada = new PeliculaImpl(dune.getId(), "Matrix", 155, Clasificacion.MAS_13);
        assertThrows(IllegalArgumentException.class, () -> gestor.actualizar(renombrada));
    }

    @Test
    void editarSinCambiarElTituloNoChocaConsigoMisma() {
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);
        dune.setDirector("Denis Villeneuve");

        assertDoesNotThrow(() -> gestor.actualizar(dune));
    }

    // ---------- editar: lo que no viene se conserva ----------

    /**
     * La edición parcial vive en el gestor y no en cada interfaz: es lo que hace que
     * editar por la web y editar por consola signifiquen lo mismo.
     */
    @Test
    void editarSoloPisaLoQueVieneEnElPedido() {
        Pelicula dune = gestor.agregar(new DatosPelicula("Dune", 155,
                List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13, "Denis Villeneuve",
                "Arrakis", 2021, "Inglés", "dune.jpg", true));

        gestor.editar(dune.getId(), DatosPelicula.deCatalogo(null, "Otra sinopsis", null, null, null));

        Pelicula leida = gestor.buscar(dune.getId()).orElseThrow();
        assertEquals("Otra sinopsis", leida.getSinopsis());
        assertEquals("Dune", leida.getTitulo());
        assertEquals(155, leida.getDuracionMinutos());
        assertEquals("Denis Villeneuve", leida.getDirector());
        assertEquals(2021, leida.getAnio());
        assertEquals(List.of(Genero.CIENCIA_FICCION), leida.getGeneros());
        assertEquals(Clasificacion.MAS_13, leida.getClasificacion());
    }

    @Test
    void elAltaCompletaGuardaElCatalogoDeUnaSolaVez() {
        Pelicula matrix = gestor.agregar(new DatosPelicula("Matrix", 136, List.of(Genero.ACCION),
                Clasificacion.MAS_13, "Wachowski", "Un hacker", 1999, "Inglés", "matrix.jpg", false));

        Pelicula leida = gestor.buscar(matrix.getId()).orElseThrow();
        assertEquals("Wachowski", leida.getDirector());
        assertEquals(1999, leida.getAnio());
        assertFalse(leida.estaEnCartelera());
        assertTrue(gestor.listarEnCartelera().isEmpty());
    }

    @Test
    void editarNoDejaRenombrarConElTituloDeOtra() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertThrows(IllegalArgumentException.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula("Matrix", null, null, null, null, null, null, null, null, null)));
    }

    /** Una edición no es una puerta de atrás: valida con las mismas reglas que el alta. */
    @Test
    void editarNoDejaUnaPeliculaSinTituloNiConDuracionCero() {
        Pelicula dune = gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        assertThrows(IllegalArgumentException.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula("  ", null, null, null, null, null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> gestor.editar(dune.getId(),
                new DatosPelicula(null, 0, null, null, null, null, null, null, null, null)));
    }

    @Test
    void editarUnaPeliculaInexistenteFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.editar(99, DatosPelicula.deCatalogo("Alguien", null, null, null, null)));
    }

    @Test
    void filtraPorGenero() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION, Genero.CIENCIA_FICCION), Clasificacion.ATP);
        gestor.agregar("Amelie", 122, List.of(Genero.ROMANCE), Clasificacion.ATP);

        assertEquals(1, gestor.listarPorGenero(Genero.CIENCIA_FICCION).size());
        assertEquals("Matrix", gestor.listarPorGenero(Genero.ACCION).get(0).getTitulo());
    }
}
