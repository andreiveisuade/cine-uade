package ar.uade.cine.service.programaciones;

import java.time.Duration;
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
import java.util.stream.Collectors;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;

// El algoritmo de la grilla, voraz: llena cada sala, de la más cara a la más barata, sin pisar R3 ni R20.
// Fabricación pura con estado por corrida: se crea una por propuesta, no es bean.
final class RepartoDePases {

    // Desigualdad del reordenamiento: la sala más cara elige primero, entre las mejor puntuadas.
    private static final Comparator<Sala> MAS_CARA_PRIMERO = Comparator
            .comparingDouble((Sala sala) -> -sala.getTipo().getMultiplicadorPrecio())
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
        List<Sala> enOrden = salas.stream().sorted(MAS_CARA_PRIMERO).toList();
        Map<Integer, CartelDeSala> carteles = enOrden.stream()
                .collect(Collectors.toMap(Sala::getId, sala -> cartelDe(sala, ventana.minutosDelDia())));
        for (LocalDate fecha : ventana.fechas()) {
            // La cobertura es del día: cada película del elenco, al menos un pase si entra.
            Set<Integer> conPaseHoy = new HashSet<>();
            for (Sala sala : enOrden) {
                llenar(sala, carteles.get(sala.getId()), agendas.get(sala.getId()),
                        ventana.primerIntento(fecha), ventana.cierre(fecha), conPaseHoy, pases);
            }
        }
        return pases;
    }

    // Turnos seguidos con su limpieza hasta que no entra ninguna; si uno choca con lo cargado, 30 minutos después.
    private void llenar(Sala sala, CartelDeSala cartel, AgendaDeSala agenda, LocalDateTime desde,
                        LocalDateTime cierre, Set<Integer> conPaseHoy, List<PaseSugerido> pases) {
        LocalDateTime momento = desde;
        while (momento.isBefore(cierre)) {
            Optional<Pelicula> candidata = laQueSigue(cartel,
                    (int) Duration.between(momento, cierre).toMinutes(), conPaseHoy);
            if (candidata.isEmpty()) {
                return;
            }
            Pelicula elegida = candidata.get();
            LocalDateTime fin = momento.plusMinutes(elegida.getDuracionMinutos());
            if (agenda.chocaEn(momento, fin)) {
                momento = momento.plusMinutes(VentanaDelDia.MINUTOS_ENTRE_INTENTOS);
                continue;
            }
            pases.add(new PaseSugerido(elegida.getId(), elegida.getTitulo(), sala.getId(), sala.getNombre(),
                    momento, elegida.getDuracionMinutos(), proyeccionEn(elegida, sala.getTipo()).orElseThrow()));
            asignados.merge(elegida.getId(), 1, Integer::sum);
            conPaseHoy.add(elegida.getId());
            momento = fin.plusMinutes(sala.getMinutosLimpieza());
        }
    }

    // Función de selección, en orden: la del cartel de la sala, la que no tiene pase hoy, la de más deuda.
    // En el último turno, dentro de ese mismo grupo, la que deja menos hueco al cierre.
    private Optional<Pelicula> laQueSigue(CartelDeSala cartel, int minutosHastaElCierre, Set<Integer> conPaseHoy) {
        List<Pelicula> entran = cartel.proyectables().stream()
                .filter(p -> p.getDuracionMinutos() <= minutosHastaElCierre)
                .toList();
        Comparator<Pelicula> grupo = Comparator
                .comparing((Pelicula p) -> !cartel.preferidas().contains(p.getId()))
                .thenComparing(p -> conPaseHoy.contains(p.getId()));
        Comparator<Pelicula> deuda = Comparator
                .comparingDouble((Pelicula p) -> asignados.get(p.getId()) / peso(p))
                .thenComparing(p -> -puntajes.de(p))
                .thenComparing(Pelicula::getTitulo);
        Optional<Pelicula> elegida = entran.stream().min(grupo.thenComparing(deuda));
        if (elegida.isEmpty() || !cartel.hueco().esElUltimo(minutosHastaElCierre,
                elegida.get().getDuracionMinutos())) {
            return elegida;
        }
        return entran.stream()
                .filter(p -> grupo.compare(p, elegida.get()) == 0)
                .min(Comparator.comparingInt((Pelicula p) -> cartel.hueco().siVa(minutosHastaElCierre,
                        p.getDuracionMinutos())).thenComparing(deuda));
    }

    // Qué puede dar la sala (R8, R31) y cuáles prefiere según su papel, que sale de los atributos de su tipo.
    private CartelDeSala cartelDe(Sala sala, int minutosDelDia) {
        List<Pelicula> proyectables = elenco.stream()
                .filter(p -> proyeccionEn(p, sala.getTipo()).isPresent())
                .toList();
        List<Pelicula> preferidas = switch (PapelEnLaGrilla.de(sala.getTipo())) {
            case MEJOR_PUNTUADAS -> mejorPuntuadas();
            case VERSIONES_3D -> versiones3D(proyectables);
            case EN_2D -> proyectables;
        };
        return new CartelDeSala(proyectables,
                preferidas.stream().map(Pelicula::getId).collect(Collectors.toSet()),
                new HuecoAlCierre(proyectables.stream().map(Pelicula::getDuracionMinutos).toList(),
                        sala.getMinutosLimpieza(), minutosDelDia));
    }

    private static List<Pelicula> versiones3D(List<Pelicula> proyectables) {
        List<Pelicula> en3D = proyectables.stream().filter(p -> p.seEstrenaEn(Proyeccion.TRES_D)).toList();
        return en3D.isEmpty() ? proyectables : en3D;
    }

    // La mitad mejor puntuada del elenco (redondeando para arriba), para que la premium no repita siempre una.
    private List<Pelicula> mejorPuntuadas() {
        return elenco.stream()
                .sorted(Comparator.comparingDouble((Pelicula p) -> -puntajes.de(p))
                        .thenComparing(Pelicula::getTitulo))
                .limit((elenco.size() + 1) / 2)
                .toList();
    }

    // En una sala que proyecta 3D va en 3D si la película tiene esa versión; si no, en 2D. Vacío: no la puede dar.
    private static Optional<Proyeccion> proyeccionEn(Pelicula pelicula, TipoSala tipo) {
        if (pelicula.seEstrenaEn(Proyeccion.TRES_D) && Proyeccion.TRES_D.sePuedeProyectarEn(tipo)) {
            return Optional.of(Proyeccion.TRES_D);
        }
        return pelicula.seEstrenaEn(Proyeccion.DOS_D) ? Optional.of(Proyeccion.DOS_D) : Optional.empty();
    }

    // Nunca cero: con puntaje 0 la deuda sería infinita y se llevaría la grilla entera.
    private double peso(Pelicula pelicula) {
        return Math.max(puntajes.de(pelicula), 0.1);
    }

    private record CartelDeSala(List<Pelicula> proyectables, Set<Integer> preferidas, HuecoAlCierre hueco) {
    }
}
