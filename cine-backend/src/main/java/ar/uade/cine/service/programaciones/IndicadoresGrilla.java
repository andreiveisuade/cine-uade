package ar.uade.cine.service.programaciones;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;

// Cuánto llena la grilla, qué tan buena y qué tan variada es; record de resultado que se calcula solo.
public record IndicadoresGrilla(int minutosProgramados, int minutosDisponibles,
                                double puntajePromedio, int generosCubiertos,
                                int generosTotales, Map<Genero, Integer> pasesPorGenero) {

    // Cuenta sobre la propuesta ya armada. Los minutos disponibles no: miran la base y el reloj, y los
    // trae el planificador.
    public static IndicadoresGrilla de(List<Pelicula> elenco, List<PaseSugerido> pases, int minutosDisponibles) {
        int programados = pases.stream().mapToInt(PaseSugerido::duracionMinutos).sum();

        Map<Integer, Pelicula> porId = new HashMap<>();
        elenco.forEach(p -> porId.put(p.getId(), p));
        double puntajeTotal = pases.stream()
                .mapToDouble(pase -> porId.get(pase.peliculaId()).getPuntaje())
                .sum();

        Map<Genero, Integer> porGenero = new EnumMap<>(Genero.class);
        for (PaseSugerido pase : pases) {
            for (Genero genero : porId.get(pase.peliculaId()).getGeneros()) {
                porGenero.merge(genero, 1, Integer::sum);
            }
        }
        Set<Genero> cubiertos = new LinkedHashSet<>();
        elenco.forEach(p -> cubiertos.addAll(p.getGeneros()));

        return new IndicadoresGrilla(programados, minutosDisponibles,
                pases.isEmpty() ? 0 : puntajeTotal / pases.size(),
                cubiertos.size(), Genero.values().length, porGenero);
    }

    public double ocupacion() {
        return minutosDisponibles == 0 ? 0 : (double) minutosProgramados / minutosDisponibles;
    }
}
