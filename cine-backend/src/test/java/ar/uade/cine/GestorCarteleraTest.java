package ar.uade.cine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.uade.cine.interfaces.Pelicula;
import ar.uade.cine.modelo.Clasificacion;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.persistencia.PeliculaDAOMemoria;
import ar.uade.cine.servicio.GestorCartelera;

/**
 * Gracias a que GestorCartelera depende de la interfaz, se puede testear la lógica
 * con el DAO en memoria: los tests corren sin MySQL levantado.
 */
class GestorCarteleraTest {

    private final GestorCartelera gestor = new GestorCartelera(new PeliculaDAOMemoria());

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

    @Test
    void laCarteleraExcluyeLasPeliculasDadasDeBaja() {
        Pelicula vieja = gestor.agregar("Titanic", 194, List.of(Genero.ROMANCE), Clasificacion.MAS_13);
        gestor.agregar("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13);

        vieja.setEnCartelera(false);
        gestor.actualizar(vieja);

        assertEquals(2, gestor.listar().size());
        assertEquals(1, gestor.listarEnCartelera().size());
        assertEquals("Dune", gestor.listarEnCartelera().get(0).getTitulo());
    }

    @Test
    void filtraPorGenero() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION, Genero.CIENCIA_FICCION), Clasificacion.ATP);
        gestor.agregar("Amelie", 122, List.of(Genero.ROMANCE), Clasificacion.ATP);

        assertEquals(1, gestor.listarPorGenero(Genero.CIENCIA_FICCION).size());
        assertEquals("Matrix", gestor.listarPorGenero(Genero.ACCION).get(0).getTitulo());
    }
}
