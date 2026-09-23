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

/**
 * Única definición de butaca ocupada en una función (R4): la usan el mapa y la venta, así
 * el mapa nunca ofrece lo que la reserva rechaza. Ocupa una reserva vigente o el bloqueo
 * temporal de quien está eligiendo ({@link #MIENTRAS_ELIGE}); al nacer la reserva,
 * {@link GestorReservas#reservar} suelta el bloqueo.
 */
@Service
public class Ocupacion {

    private static final Logger LOG = LoggerFactory.getLogger(Ocupacion.class);

    /**
     * Corto a propósito: se renueva con cada toque al mapa y acota cuánto retiene quien
     * cerró la pestaña. Es regla de negocio, por eso vive acá y no en el adaptador.
     */
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

    /** Canceladas y expiradas liberan las suyas (R6). */
    public Set<Integer> asientosOcupados(int funcionId) {
        return asientosOcupados(funcionId, null);
    }

    /** Sin contar las que bloqueó esa sesión: a ella no le están ocupadas. */
    public Set<Integer> asientosOcupados(int funcionId, String sesion) {
        List<Reserva> reservas = reservaRepository.findByFuncionId(funcionId);
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

    /** Única definición de "libre": habilitada (R9) y no ocupada. */
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

    /**
     * Recibe la selección entera para ser idempotente: toma, renueva y suelta de una vez.
     * No conseguir una butaca no es error (otro llegó primero): quien llama compara.
     *
     * @return los códigos que quedaron a nombre de esa sesión
     */
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
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
        return asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(funcion.getSalaId());
    }

    /**
     * R17 sin scheduler: expira quien consulta. Escribe y no solo deriva el estado porque
     * el {@code UNIQUE (funcion_id, asiento_id)} no sabe de vencimientos.
     */
    private void expirarVencidas(List<Reserva> reservas) {
        LocalDateTime ahora = reloj.ahora();
        for (Reserva reserva : reservas) {
            if (reserva.estaVencida(ahora)) {
                reserva.expirar();
                reservaRepository.save(reserva);
                // Pasa sin usuario del otro lado: el log responde el reclamo de un cliente.
                LOG.info("reserva {} EXPIRADA · creada {} · {} butacas vuelven a la venta",
                        reserva.getId(), reserva.getCreadaEn(), reserva.getCantidadEntradas());
            }
        }
    }
}
