package ar.uade.cine.service.programaciones;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;

public record PropuestaGrilla(List<Pelicula> elenco, List<PaseSugerido> pases,
                              IndicadoresGrilla indicadores) {

    public record PaseSugerido(int peliculaId, String titulo, int salaId, String sala,
                               LocalDateTime inicio, int duracionMinutos) {
    }

    public record IndicadoresGrilla(int minutosProgramados, int minutosDisponibles,
                                    double puntajePromedio, int generosCubiertos,
                                    int generosTotales, Map<Genero, Integer> pasesPorGenero) {

        public double ocupacion() {
            return minutosDisponibles == 0 ? 0 : (double) minutosProgramados / minutosDisponibles;
        }
    }
}
