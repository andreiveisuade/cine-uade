package ar.uade.cine.service.ventas;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.ventas.BloqueoButaca;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.SesionDeCompra;
import ar.uade.cine.repository.salas.AsientoRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.ventas.BloqueoButacaRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.infrastructure.reloj.Reloj;

// Única definición de butaca ocupada (R4), la del mapa y la venta; reservas vigentes más bloqueos ajenos.
@Service
@RequiredArgsConstructor
@Slf4j
public class Ocupacion {

    public static final Duration MIENTRAS_ELIGE = Duration.ofMinutes(3);

    // R19 dice lo mismo al bloquear y al vender: los dos pasan por funcionEnVenta.
    private static final String FUNCION_EMPEZADA = "La función ya empezó: no se pueden reservar butacas";

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;
    private final AsientoRepository asientoRepository;
    private final BloqueoButacaRepository bloqueos;
    private final Reloj reloj;

    public Set<Integer> asientosOcupados(int funcionId, String sesion) {
        String propia = SesionDeCompra.comoSeGuarda(sesion);
        List<Reserva> reservas = reservaRepository.findByFuncion_Id(funcionId);
        expirarVencidas(reservas);
        Set<Integer> ocupados = reservas.stream()
                .filter(Reserva::estaVigente)
                .flatMap(r -> r.getEntradas().stream())
                .map(Entrada::asientoId)
                .collect(Collectors.toCollection(HashSet::new));
        for (BloqueoButaca bloqueo : bloqueos.vigentes(funcionId, reloj.ahora())) {
            if (!bloqueo.sesion().equals(propia)) {
                ocupados.add(bloqueo.asientoId());
            }
        }
        return ocupados;
    }

    public static List<Asiento> libresEntre(List<Asiento> asientos, Set<Integer> ocupados) {
        return asientos.stream()
                .filter(a -> !a.estaFueraDeServicio())
                .filter(a -> !ocupados.contains(a.getId()))
                .toList();
    }

    // Perder una butaca no es un error: vuelve en rechazadas, con el código ya normalizado. La sesión, como
    // quedó guardada: es la que el cliente tiene que mandar al reservar.
    public record Bloqueo(String sesion, List<String> conseguidas, List<String> rechazadas) {
    }

    public Bloqueo bloquear(int funcionId, Collection<String> codigos, String textoSesion) {
        String sesion = new SesionDeCompra(textoSesion).valor();
        List<Asiento> pedidos = butacasPedidas(funcionEnVenta(funcionId), codigos);
        Set<Integer> ocupados = asientosOcupados(funcionId, sesion);
        LocalDateTime ahora = reloj.ahora();

        List<String> conseguidas = pedidos.stream()
                .filter(a -> !ocupados.contains(a.getId()))
                .filter(a -> tomar(funcionId, a.getId(), sesion, ahora))
                .map(Asiento::getCodigo)
                .toList();
        List<String> rechazadas = pedidos.stream()
                .map(Asiento::getCodigo)
                .filter(codigo -> !conseguidas.contains(codigo))
                .toList();

        Set<Integer> sigueEligiendo = pedidos.stream().map(Asiento::getId).collect(Collectors.toSet());
        if (sigueEligiendo.isEmpty()) {
            liberar(funcionId, sesion);
        } else {
            bloqueos.liberarMenos(funcionId, sesion, sigueEligiendo);
        }
        return new Bloqueo(sesion, conseguidas, rechazadas);
    }

    public void liberar(int funcionId, String sesion) {
        bloqueos.liberar(funcionId, SesionDeCompra.comoSeGuarda(sesion));
    }

    // Solo higiene: una fila vencida ya no ocupa nada, porque vigentes() filtra por
    // vencimiento y renovarOTomarVencida() la pisa. Sin esto la tabla crecería con cada
    // mapa abandonado. En el perfil test no corre: ver Adaptadores.Tareas.
    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void borrarBloqueosVencidos() {
        bloqueos.borrarVencidos(reloj.ahora());
    }

    // Sin leer antes de escribir: entre un SELECT que dice "libre" y el INSERT cabe otra
    // sesión. Por qué alcanzan estas dos sentencias, en BloqueoButacaRepository.
    private boolean tomar(int funcionId, int asientoId, String sesion, LocalDateTime ahora) {
        LocalDateTime vence = ahora.plus(MIENTRAS_ELIGE);
        return bloqueos.renovarOTomarVencida(funcionId, asientoId, sesion, vence, ahora) > 0
                || bloqueos.insertarSiNoEsta(funcionId, asientoId, sesion, vence) > 0;
    }

    // R19: una función que ya arrancó no se vende, así que tampoco se le apartan butacas. Va antes que
    // cualquier otra regla de la venta porque las anula a todas.
    public Funcion funcionEnVenta(int funcionId) {
        Funcion funcion = funcionRepository.exigir(funcionId, "la función");
        if (funcion.yaEmpezo(reloj.ahora())) {
            throw new DatoInvalido(FUNCION_EMPEZADA);
        }
        return funcion;
    }

    // Las butacas del pedido, con las reglas de la venta: que sean de la sala, que estén en servicio (R9)
    // y hasta el tope de una compra. Antes se apartaba una fuera de servicio que después no se podía comprar.
    // distinct() alcanza para "a1" y "A1": exigirConCodigo devuelve la misma instancia de deLaSala.
    private List<Asiento> butacasPedidas(Funcion funcion, Collection<String> codigos) {
        List<Asiento> deLaSala = asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(funcion.getSalaId());
        List<Asiento> pedidas = codigos == null ? List.of() : codigos.stream()
                .map(codigo -> Asiento.exigirConCodigo(deLaSala, codigo))
                .distinct()
                .toList();
        pedidas.forEach(Asiento::exigirEnServicio);
        Reserva.validarTopeDeButacas(pedidas);
        return pedidas;
    }

    // R17 sin scheduler: expira quien consulta. Escribe porque el UNIQUE no sabe de vencimientos.
    private void expirarVencidas(List<Reserva> reservas) {
        LocalDateTime ahora = reloj.ahora();
        for (Reserva reserva : reservas) {
            if (reserva.estaVencida(ahora)) {
                reserva.expirar();
                reservaRepository.save(reserva);
                log.info("reserva {} EXPIRADA · creada {} · {} butacas vuelven a la venta",
                        reserva.getId(), reserva.getCreadaEn(), reserva.getCantidadEntradas());
            }
        }
    }
}
