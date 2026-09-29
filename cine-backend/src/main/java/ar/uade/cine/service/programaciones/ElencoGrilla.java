package ar.uade.cine.service.programaciones;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;

// Qué películas entran a la grilla: la mejor, y después la que más aporta; Fabricación pura, sin base.
// Aparte del planificador porque es cuenta sobre una lista, como PuntajeConfiable.
final class ElencoGrilla {

    // Crece con la raíz porque TMDB etiqueta de más.
    private static final double BONO_GENERO_NUEVO = 2.0;

    private ElencoGrilla() {
    }

    // Una sin duración no entra: no se le puede armar ningún pase.
    static List<Pelicula> elegir(List<Pelicula> confirmadas, int cuantas) {
        List<Pelicula> candidatas = new ArrayList<>(confirmadas.stream()
                .filter(p -> p.getDuracionMinutos() > 0)
                .toList());

        PuntajeConfiable puntajes = new PuntajeConfiable(candidatas);
        List<Pelicula> elenco = new ArrayList<>();
        Set<Genero> cubiertos = new HashSet<>();
        while (elenco.size() < cuantas && !candidatas.isEmpty()) {
            Set<Genero> yaCubiertos = Set.copyOf(cubiertos);
            Pelicula mejor = candidatas.stream()
                    .max(Comparator.comparingDouble((Pelicula p) -> valor(p, yaCubiertos, elenco.isEmpty(), puntajes))
                            // Desempate por título, para que la propuesta sea reproducible.
                            .thenComparing(Pelicula::getTitulo, Comparator.reverseOrder()))
                    .orElseThrow();
            elenco.add(mejor);
            cubiertos.addAll(mejor.getGeneros());
            candidatas.remove(mejor);
        }
        return elenco;
    }

    // La primera no lleva bono: premiaría a la que tiene más etiquetas de TMDB.
    private static double valor(Pelicula pelicula, Set<Genero> cubiertos, boolean primera,
                                PuntajeConfiable puntajes) {
        double puntaje = puntajes.de(pelicula);
        if (primera) {
            return puntaje;
        }
        long nuevos = pelicula.getGeneros().stream().filter(g -> !cubiertos.contains(g)).count();
        return puntaje + BONO_GENERO_NUEVO * Math.sqrt(nuevos);
    }
}
