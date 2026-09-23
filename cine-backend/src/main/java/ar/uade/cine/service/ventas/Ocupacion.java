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
 * Qué butacas están tomadas en una función y cuáles quedan. Es la <strong>única</strong>
 * definición de "ocupado": el mapa de la API y {@link GestorReservas} al vender preguntan
 * acá, así el mapa nunca ofrece una butaca que la reserva después rechaza. Vive aparte
 * porque el sujeto es la función, no la reserva ni la sala.
 *
 * <p>Una butaca está tomada por dos motivos distintos: alguien la <strong>compró</strong>
 * (reserva vigente, dura {@link Reserva#MINUTOS_PARA_PAGAR} minutos sin pagar, deja
 * historial y ticket) o alguien la <strong>está eligiendo</strong> (bloqueo de una sesión
 * anónima, dura {@link #MIENTRAS_ELIGE}, se borra solo). No se puede reemplazar una por
 * la otra: la reserva necesita un cliente que recién se identifica al confirmar, y el
 * bloqueo no sobrevive a un reinicio. Nunca se solapan: al nacer la reserva,
 * {@link GestorReservas#reservar} suelta el bloqueo.
 */
@Service
public class Ocupacion {

    private static final Logger LOG = LoggerFactory.getLogger(Ocupacion.class);

    /**
     * Cuánto se le guarda una butaca a quien la está eligiendo. Regla de negocio, así que
     * vive acá y no en el adaptador. Corta a propósito: se renueva con cada toque al
     * mapa, y lo que acota es cuánto retiene alguien que cerró la pestaña.
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

    /**
     * Ids de las butacas tomadas en esa función. Las reservas canceladas y las expiradas
     * liberan las suyas (R6), y los bloqueos vencidos también.
     */
    public Set<Integer> asientosOcupados(int funcionId) {
        return asientosOcupados(funcionId, null);
    }

    /**
     * Lo mismo, sin contar las butacas que bloqueó esa sesión: las suyas no le están
     * ocupadas a ella. Así el mapa se las muestra elegidas y reservar no se las rechaza.
     */
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

    /** Butacas de la sala que todavía nadie tomó para esa función. */
    public List<Asiento> asientosLibres(int funcionId) {
        return asientosLibres(funcionId, null);
    }

    public List<Asiento> asientosLibres(int funcionId, String sesion) {
        return libresEntre(asientosDeLaSala(funcionId), asientosOcupados(funcionId, sesion));
    }

    /** Las que se pueden vender: habilitadas y que nadie tomó. Única definición de "libre". */
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
     * Le guarda a esa sesión las butacas que está eligiendo y le suelta las que dejó de
     * elegir. Recibe la selección entera y no una butaca suelta para ser idempotente: el
     * navegador manda lo elegido en cada toque, y eso toma, renueva y suelta de una vez.
     * Con "tomar" y "soltar" separados, una pestaña cerrada dejaba butacas sin soltar.
     *
     * <p>Que una butaca no se consiga no es un error, es que otro llegó primero: devuelve
     * lo que consiguió y quien llama compara contra lo que pidió.
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
                        // Mismo mensaje que al reservar: para quien elige es la misma butaca inexistente.
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

    /** Suelta todo lo que esa sesión tenga bloqueado en esa función. */
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
     * Cierra las reservas que nadie pagó a tiempo, y con eso devuelve sus butacas a la
     * venta. No hay scheduler: la limpieza la hace quien consulta, que es cuando importa.
     *
     * <p>Tiene que escribir y no solo derivar el estado al vuelo: el {@code UNIQUE
     * (funcion_id, asiento_id)} no sabe de vencimientos, y mientras la entrada conserve su
     * funcion_id la butaca está libre en la teoría y bloqueada en la práctica. El bloqueo
     * de quien elige no necesita esto: vence solo donde vive y no deja fila que corregir.
     */
    private void expirarVencidas(List<Reserva> reservas) {
        LocalDateTime ahora = reloj.ahora();
        for (Reserva reserva : reservas) {
            if (reserva.estaVencida(ahora)) {
                reserva.expirar();
                reservaRepository.save(reserva);
                // Pasa sola, sin usuario del otro lado: sin registro, una butaca liberada
                // parece magia el día que un cliente reclama que la tenía reservada.
                LOG.info("reserva {} EXPIRADA · creada {} · {} butacas vuelven a la venta",
                        reserva.getId(), reserva.getCreadaEn(), reserva.getCantidadEntradas());
            }
        }
    }
}
