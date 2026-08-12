package ar.uade.cine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

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
        gestor.agregar("El Padrino", 175, List.of(Genero.DRAMA));
        assertEquals(1, gestor.listar().size());
        assertTrue(gestor.buscar(1).isPresent());
    }

    @Test
    void rechazaTituloRepetido() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION));
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("matrix", 136, List.of(Genero.ACCION)));
    }

    @Test
    void rechazaDuracionInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("Sin duración", 0, List.of(Genero.DRAMA)));
    }

    @Test
    void rechazaPeliculaSinGenero() {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.agregar("Sin género", 100, List.of()));
    }

    @Test
    void filtraPorGenero() {
        gestor.agregar("Matrix", 136, List.of(Genero.ACCION, Genero.CIENCIA_FICCION));
        gestor.agregar("Amelie", 122, List.of(Genero.ROMANCE));

        assertEquals(1, gestor.listarPorGenero(Genero.CIENCIA_FICCION).size());
        assertEquals("Matrix", gestor.listarPorGenero(Genero.ACCION).get(0).getTitulo());
    }
}
