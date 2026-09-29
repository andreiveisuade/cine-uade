package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;

class PuntajeConfiableTest {

    private static Pelicula pelicula(double puntaje, int votos) {
        Pelicula pelicula = new Pelicula("Una", 100, List.of(Genero.DRAMA), Clasificacion.ATP);
        pelicula.cambiarCatalogo(pelicula.getCatalogo()
                .conCambios(null, null, null, null, null, puntaje, votos, LocalDate.of(2026, 8, 13)));
        return pelicula;
    }

    // Ponderado por votos: (8 × 150 + 4 × 50) / 200 = 7.
    private final PuntajeConfiable puntajes = new PuntajeConfiable(List.of(pelicula(8, 150), pelicula(4, 50)));

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin votos ni puntaje vale el promedio del catálogo,    0, 0,    7
            sin votos con puntaje lo cargó el encargado: se respeta, 9, 0,    9
            con 50 votos pesa mitad su nota y mitad el promedio,     9, 50,   8
            con muchos votos vale casi su nota,                      9, 4950, 8.98
            """)
    void elPuntajeSeAcercaAlPromedioCuantoMenosVotosTiene(String caso, double puntaje, int votos,
            double esperado) {
        assertEquals(esperado, puntajes.de(pelicula(puntaje, votos)), 0.001, caso);
    }

    @Test
    void sinVotosEnElCatalogoElPromedioIgnoraLasQueNoTienenPuntaje() {
        PuntajeConfiable sinVotos = new PuntajeConfiable(List.of(pelicula(8, 0), pelicula(6, 0), pelicula(0, 0)));

        assertEquals(7, sinVotos.de(pelicula(0, 0)), 0.001);
    }
}
