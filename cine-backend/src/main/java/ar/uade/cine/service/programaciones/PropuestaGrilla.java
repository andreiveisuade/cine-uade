package ar.uade.cine.service.programaciones;

import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.model.cartelera.Pelicula;

// Elenco, pases e indicadores de una grilla automática; record de resultado, el mismo al proponer y aplicar.
public record PropuestaGrilla(List<Pelicula> elenco, List<PaseSugerido> pases,
                              IndicadoresGrilla indicadores) {

    public record PaseSugerido(int peliculaId, String titulo, int salaId, String sala,
                               LocalDateTime inicio, int duracionMinutos) {
    }

    public int pasesDe(int peliculaId) {
        return (int) pases.stream().filter(pase -> pase.peliculaId() == peliculaId).count();
    }
}
