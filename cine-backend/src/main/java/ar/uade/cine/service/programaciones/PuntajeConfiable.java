package ar.uade.cine.service.programaciones;

import java.util.List;

import ar.uade.cine.model.cartelera.Pelicula;

// Puntaje de una película corregido por sus votos; Fabricación pura: promedio bayesiano contra el catálogo.
// Con pocos votos se acerca al promedio en vez de valer solo. Aparte del planificador porque es cuenta
// pura, sin base ni reloj.
final class PuntajeConfiable {

    // Votos desde los cuales el puntaje vale solo; con menos, pesa el promedio del catálogo.
    private static final int VOTOS_PARA_CONFIAR = 50;

    private final double promedio;

    PuntajeConfiable(List<Pelicula> catalogo) {
        this.promedio = promedioDe(catalogo);
    }

    // (v / (v + m)) × nota + (m / (v + m)) × promedio.
    double de(Pelicula pelicula) {
        int votos = pelicula.getVotos();
        if (votos <= 0) {
            // Con puntaje y sin votos la cargó el encargado a mano: su criterio no se corrige.
            return pelicula.getPuntaje() > 0 ? pelicula.getPuntaje() : promedio;
        }
        double peso = (double) votos / (votos + VOTOS_PARA_CONFIAR);
        return peso * pelicula.getPuntaje() + (1 - peso) * promedio;
    }

    // Ponderado por votos; los ceros de las no votadas hundirían la referencia.
    private static double promedioDe(List<Pelicula> catalogo) {
        double votos = catalogo.stream().mapToDouble(Pelicula::getVotos).sum();
        if (votos > 0) {
            return catalogo.stream()
                    .mapToDouble(p -> p.getPuntaje() * p.getVotos())
                    .sum() / votos;
        }
        return catalogo.stream()
                .mapToDouble(Pelicula::getPuntaje)
                .filter(p -> p > 0)
                .average()
                .orElse(0);
    }
}
