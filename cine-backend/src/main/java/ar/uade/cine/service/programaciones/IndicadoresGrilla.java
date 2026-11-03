package ar.uade.cine.service.programaciones;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;

// Cuánto llena la grilla, cuánto factura, qué tan buena y qué tan variada es; record de resultado.
public record IndicadoresGrilla(int minutosProgramados, int minutosDisponibles,
                                double puntajePromedio, int generosCubiertos,
                                int generosTotales, Map<Genero, Integer> pasesPorGenero,
                                Dinero facturacionEsperada) {

    // Cuenta sobre la propuesta ya armada. Los minutos disponibles y la sala llena no: miran la base y el
    // reloj, y los trae el planificador.
    static IndicadoresGrilla de(List<Pelicula> elenco, List<PaseSugerido> pases, int minutosDisponibles,
                                Function<PaseSugerido, Dinero> aSalaLlena, PuntajeConfiable puntajes) {
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

        // Estimación: no hay datos de demanda, así que se supone que la sala se llena en proporción al puntaje.
        Dinero facturacion = pases.stream()
                .map(pase -> aSalaLlena.apply(pase)
                        .por(Math.min(puntajes.de(porId.get(pase.peliculaId())) / 10, 1)))
                .reduce(Dinero.CERO, Dinero::mas);

        return new IndicadoresGrilla(programados, minutosDisponibles,
                pases.isEmpty() ? 0 : puntajeTotal / pases.size(),
                cubiertos.size(), Genero.values().length, porGenero, facturacion);
    }

    public double ocupacion() {
        return minutosDisponibles == 0 ? 0 : (double) minutosProgramados / minutosDisponibles;
    }
}
