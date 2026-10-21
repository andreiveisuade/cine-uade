package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;

// El algoritmo de la grilla: cada turno libre va a la película con más deuda, sin pisar la agenda (R3).
// Fabricación pura con estado por corrida: se crea una por propuesta, no es bean.
final class RepartoDePases {

    private final List<Pelicula> elenco;
    private final PuntajeConfiable puntajes;
    private final Map<Integer, Integer> asignados = new HashMap<>();

    RepartoDePases(List<Pelicula> elenco, PuntajeConfiable puntajes) {
        this.elenco = elenco;
        this.puntajes = puntajes;
        elenco.forEach(p -> asignados.put(p.getId(), 0));
    }

    List<PaseSugerido> repartir(List<Sala> salas, Map<Integer, AgendaDeSala> agendas, VentanaDelDia ventana) {
        List<PaseSugerido> pases = new ArrayList<>();
        for (LocalDate fecha : ventana.fechas()) {
            for (Sala sala : salas) {
                LocalDateTime momento = ventana.primerIntento(fecha);
                LocalDateTime limite = ventana.cierre(fecha);

                while (momento.isBefore(limite)) {
                    Pelicula elegida = conMasDeuda();
                    LocalDateTime fin = momento.plusMinutes(elegida.getDuracionMinutos());
                    if (fin.isAfter(limite)) {
                        // No se prueba una más corta: se llevaría siempre el último turno.
                        break;
                    }
                    if (agendas.get(sala.getId()).chocaEn(momento, fin)) {
                        momento = momento.plusMinutes(VentanaDelDia.MINUTOS_ENTRE_INTENTOS);
                        continue;
                    }
                    pases.add(new PaseSugerido(elegida.getId(), elegida.getTitulo(),
                            sala.getId(), sala.getNombre(), momento, elegida.getDuracionMinutos()));
                    asignados.merge(elegida.getId(), 1, Integer::sum);
                    momento = fin.plusMinutes(sala.getMinutosLimpieza());
                }
            }
        }
        return pases;
    }

    private Pelicula conMasDeuda() {
        return elenco.stream()
                .min(Comparator.comparingDouble((Pelicula p) -> asignados.get(p.getId()) / peso(p))
                        .thenComparing(Pelicula::getTitulo))
                .orElseThrow();
    }

    // Nunca cero: con puntaje 0 la deuda sería infinita y se llevaría la grilla entera.
    private double peso(Pelicula pelicula) {
        return Math.max(puntajes.de(pelicula), 0.1);
    }
}
