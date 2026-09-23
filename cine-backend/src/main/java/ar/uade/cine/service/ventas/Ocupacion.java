package ar.uade.cine.service.ventas;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.EstadoAsiento;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.AsientoRepository;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacas;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.service.RecursoNoEncontrado;

// Única definición de butaca ocupada (R4): la usan el mapa y la venta.
@Service
public class Ocupacion {

    private static final Logger LOG = LoggerFactory.getLogger(Ocupacion.class);

    public static final Duration MIENTRAS_ELIGE = Duration.ofMinutes(3);

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;
    private final AsientoRepository asientoRepository;
    private final BloqueoButacas bloqueos;
    private final Reloj reloj;

    public Ocupacion(ReservaRepository reservaRepository, FuncionRepository funcionRepository, AsientoRepository asientoRepository,
                     BloqueoButacas bloqueos, Reloj reloj) {
        this.reservaRepository = reservaRepository;
        this.funcionRepository = funcionRepository;
        this.asientoRepository = asientoRepository;
        this.bloqueos = bloqueos;
        this.reloj = reloj;
    }

    public Set<Integer> asientosOcupados(int funcionId) {
        return asientosOcupados(funcionId, null);
    }

    public Set<Integer> asientosOcupados(int funcionId, String sesion) {
        List<Reserva> reservas = reservaRepository.findByFuncion_Id(funcionId);
        expirarVencidas(reservas);
        Set<Integer> ocupados = reservas.stream()
                .filter(Reserva::estaVigente)
                .flatMap(r -> r.getEntradas().stream())
                .map(Entrada::asientoId)
                .collect(Collectors.toCollection(HashSet::new));
        bloqueos.bloqueadas(funcionId).forEach((asientoId, duenio) -> {
            if (!duenio.equals(sesion)) {
                ocupados.add(asientoId);
            }
        });
        return ocupados;
    }

    public List<Asiento> asientosLibres(int funcionId) {
        return asientosLibres(funcionId, null);
    }

    public List<Asiento> asientosLibres(int funcionId, String sesion) {
        return libresEntre(asientosDeLaSala(funcionId), asientosOcupados(funcionId, sesion));
    }

    public static List<Asiento> libresEntre(List<Asiento> asientos, Set<Integer> ocupados) {
        return asientos.stream()
                .filter(a -> a.getEstado() != EstadoAsiento.FUERA_DE_SERVICIO)
                .filter(a -> !ocupados.contains(a.getId()))
                .toList();
    }

    public int lugaresLibres(int funcionId) {
        return asientosLibres(funcionId).size();
    }

    public int lugaresLibres(int funcionId, String sesion) {
        return asientosLibres(funcionId, sesion).size();
    }

    public List<String> bloquear(int funcionId, Collection<String> codigos, String sesion) {
        if (sesion == null || sesion.isBlank()) {
            throw new IllegalArgumentException("Hace falta una sesión para bloquear butacas");
        }
        List<Asiento> deLaSala = asientosDeLaSala(funcionId);
        Set<Integer> ocupados = asientosOcupados(funcionId, sesion);

        List<Asiento> pedidos = codigos == null ? List.of() : codigos.stream()
                .map(codigo -> Asiento.conCodigo(deLaSala, codigo)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "La butaca " + Asiento.normalizarCodigo(codigo) + " no existe en esa sala")))
                .toList();

        List<String> conseguidas = pedidos.stream()
                .filter(a -> !ocupados.contains(a.getId()))
                .filter(a -> bloqueos.bloquear(funcionId, a.getId(), sesion, MIENTRAS_ELIGE))
                .map(Asiento::getCodigo)
                .toList();

        Set<Integer> sigueEligiendo = pedidos.stream().map(Asiento::getId).collect(Collectors.toSet());
        soltarDeLaSesion(funcionId, sesion, asientoId -> !sigueEligiendo.contains(asientoId));
        return conseguidas;
    }

    public void liberar(int funcionId, String sesion) {
        soltarDeLaSesion(funcionId, sesion, asientoId -> true);
    }

    private void soltarDeLaSesion(int funcionId, String sesion, IntPredicate corresponde) {
        for (Map.Entry<Integer, String> bloqueada : bloqueos.bloqueadas(funcionId).entrySet()) {
            if (bloqueada.getValue().equals(sesion) && corresponde.test(bloqueada.getKey())) {
                bloqueos.liberar(funcionId, bloqueada.getKey(), sesion);
            }
        }
    }

    private List<Asiento> asientosDeLaSala(int funcionId) {
        Funcion funcion = funcionRepository.findById(funcionId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la función " + funcionId));
        return asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(funcion.getSalaId());
    }

    // R17 sin scheduler: expira quien consulta. Escribe porque el UNIQUE no sabe de vencimientos.
    private void expirarVencidas(List<Reserva> reservas) {
        LocalDateTime ahora = reloj.ahora();
        for (Reserva reserva : reservas) {
            if (reserva.estaVencida(ahora)) {
                reserva.expirar();
                reservaRepository.save(reserva);
                LOG.info("reserva {} EXPIRADA · creada {} · {} butacas vuelven a la venta",
                        reserva.getId(), reserva.getCreadaEn(), reserva.getCantidadEntradas());
            }
        }
    }
}
