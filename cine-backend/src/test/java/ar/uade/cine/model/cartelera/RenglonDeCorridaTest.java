package ar.uade.cine.model.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.uade.cine.model.cartelera.RenglonDeCorrida.Resultado;

class RenglonDeCorridaTest {

    @Test
    void loQueSeEscribeSeLeeIgual() {
        List<RenglonDeCorrida> escritos = List.of(
                RenglonDeCorrida.nueva(7, "Los Juegos del Hambre: Sinsajo - Parte 1"),
                RenglonDeCorrida.yaEstaba("Cars"),
                RenglonDeCorrida.rechazada("Yo, narciso: el regreso", "La duración tiene que estar entre 1 y 600"));
        String detalle = String.join("\n", escritos.stream().map(RenglonDeCorrida::texto).toList());

        assertEquals(escritos, RenglonDeCorrida.leer(detalle));
    }

    @Test
    void elDetalleDeUnaCorridaFallidaNoTieneRenglones() {
        assertTrue(RenglonDeCorrida.leer("TMDB rechazó el token: revisá TMDB_TOKEN").isEmpty());
        assertTrue(RenglonDeCorrida.leer(null).isEmpty());
    }

    // Antes del tabulador el motivo iba tras «: »: no se sabe dónde termina el título, queda entero.
    @Test
    void unaRechazadaViejaQuedaConTodoComoTitulo() {
        RenglonDeCorrida vieja = RenglonDeCorrida.leer("✗ Rota: No se pudo guardar").get(0);

        assertEquals(Resultado.RECHAZADA, vieja.resultado());
        assertEquals("Rota: No se pudo guardar", vieja.titulo());
        assertEquals(null, vieja.motivo());
    }
}
