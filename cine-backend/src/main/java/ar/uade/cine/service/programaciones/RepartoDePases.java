package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;

// El algoritmo de la grilla, voraz: cada turno va a la película con más deuda que entra, sin pisar R3.
// Fabricación pura con estado por corrida: se crea una por propuesta, no es bean.
final class RepartoDePases {

    // 2D primero: la sala más barata que proyecta el formato; IMAX y 4D, solo si el elenco no entró antes.
    private static final Comparator<Sala> MAS_BARATA_PRIMERO = Comparator
            .comparingDouble((Sala sala) -> sala.getTipo().getMultiplicadorPrecio())
            .thenComparingInt(Sala::getId);

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
        List<Sala> enOrden = salas.stream().sorted(MAS_BARATA_PRIMERO).toList();
        for (LocalDate fecha : ventana.fechas()) {
            Set<Integer> conPaseHoy = new HashSet<>();
            for (Sala sala : enOrden) {
                // Con todo el elenco en cartel ese día, no se abre una sala más cara.
                if (conPaseHoy.size() == elenco.size()) {
                    break;
                }
                LocalDateTime momento = ventana.primerIntento(fecha);
                LocalDateTime limite = ventana.cierre(fecha);

                while (momento.isBefore(limite)) {
                    Optional<Pelicula> candidata = laQueSigue(momento, limite, conPaseHoy);
                    if (candidata.isEmpty()) {
                        break;
                    }
                    Pelicula elegida = candidata.get();
                    LocalDateTime fin = momento.plusMinutes(elegida.getDuracionMinutos());
                    if (agendas.get(sala.getId()).chocaEn(momento, fin)) {
                        momento = momento.plusMinutes(VentanaDelDia.MINUTOS_ENTRE_INTENTOS);
                        continue;
                    }
                    pases.add(new PaseSugerido(elegida.getId(), elegida.getTitulo(),
                            sala.getId(), sala.getNombre(), momento, elegida.getDuracionMinutos()));
                    asignados.merge(elegida.getId(), 1, Integer::sum);
                    conPaseHoy.add(elegida.getId());
                    momento = fin.plusMinutes(sala.getMinutosLimpieza());
                }
            }
        }
        return pases;
    }

    // Esquema voraz: la que no termina antes del cierre se descarta y se prueba la siguiente.
    // Primero las que todavía no tienen pase hoy; entre ellas, la de más deuda.
    private Optional<Pelicula> laQueSigue(LocalDateTime momento, LocalDateTime limite, Set<Integer> conPaseHoy) {
        return elenco.stream()
                .filter(p -> !momento.plusMinutes(p.getDuracionMinutos()).isAfter(limite))
                .min(Comparator.comparing((Pelicula p) -> conPaseHoy.contains(p.getId()))
                        .thenComparingDouble(p -> asignados.get(p.getId()) / peso(p))
                        .thenComparing(Pelicula::getTitulo));
    }

    // Nunca cero: con puntaje 0 la deuda sería infinita y se llevaría la grilla entera.
    private double peso(Pelicula pelicula) {
        return Math.max(puntajes.de(pelicula), 0.1);
    }
}
