package ar.uade.cine.service.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;

class DatosPeliculaBuilderTest {

    @Test
    void cadaDatoVaASuComponente() {
        DatosPelicula datos = DatosPelicula.con().titulo("Matrix").duracion(136).generos(List.of(Genero.ACCION))
                .clasificacion(Clasificacion.MAS_13).director("Wachowski").sinopsis("Un hacker").anio(1999)
                .idiomaOriginal("Inglés").posterUrl("https://matrix.jpg").enCartelera(true).puntaje(8.7).votos(4300)
                .armar();

        assertEquals(new DatosPelicula("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13, "Wachowski",
                "Un hacker", 1999, "Inglés", "https://matrix.jpg", true, 8.7, 4300, null), datos);
    }

    @Test
    void loQueNoSeNombraViajaNull() {
        assertEquals(new DatosPelicula(null, null, null, null, "Denis Villeneuve", null, null, null, null, null,
                null, null, null), DatosPelicula.con().director("Denis Villeneuve").armar());
    }
}
