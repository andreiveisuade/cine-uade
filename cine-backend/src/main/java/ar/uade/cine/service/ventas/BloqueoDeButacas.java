package ar.uade.cine.service.ventas;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.SesionDeCompra;
import ar.uade.cine.repository.salas.AsientoRepository;
import ar.uade.cine.repository.ventas.BloqueoButacaRepository;

// Bloqueo efímero de butacas mientras el cliente elige: tomar, soltar y limpiar los vencidos. Solo UX: la venta
// la decide la reserva.
// Alta cohesión (GRASP) / SRP: salió de Ocupacion junto con su @Scheduled; cambia si cambia cómo se elige en el mapa.
@Service
@RequiredArgsConstructor
public class BloqueoDeButacas {

    public static final Duration MIENTRAS_ELIGE = Duration.ofMinutes(3);

    private final FuncionesEnVenta enVenta;
    private final Ocupacion ocupacion;
    private final AsientoRepository asientoRepository;
    private final BloqueoButacaRepository bloqueos;
    private final Reloj reloj;

    // Orden de la venta: R19, después el pedido (sala, R9, tope) y recién ahí la ocupación (R4).
    public ButacasBloqueadas bloquear(int funcionId, Collection<String> codigos, String textoSesion) {
        String sesion = new SesionDeCompra(textoSesion).valor();
        List<Asiento> pedidos = butacasPedidas(enVenta.exigir(funcionId), codigos);
        Set<Integer> ocupados = ocupacion.asientosOcupados(funcionId, sesion);
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
        return new ButacasBloqueadas(sesion, conseguidas, rechazadas);
    }

    public void liberar(int funcionId, String sesion) {
        bloqueos.liberar(funcionId, SesionDeCompra.comoSeGuarda(sesion));
    }

    // Solo higiene: una fila vencida ya no ocupa nada, porque vigentes() filtra por
    // vencimiento y renovarOTomarVencida() la pisa. Sin esto la tabla crecería con cada
    // mapa abandonado. En el perfil test no corre: ver Adaptadores.Tareas.
    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void borrarVencidos() {
        bloqueos.borrarVencidos(reloj.ahora());
    }

    // Sin leer antes de escribir: entre un SELECT que dice "libre" y el INSERT cabe otra
    // sesión. Por qué alcanzan estas dos sentencias, en BloqueoButacaRepository.
    private boolean tomar(int funcionId, int asientoId, String sesion, LocalDateTime ahora) {
        LocalDateTime vence = ahora.plus(MIENTRAS_ELIGE);
        return bloqueos.renovarOTomarVencida(funcionId, asientoId, sesion, vence, ahora) > 0
                || bloqueos.insertarSiNoEsta(funcionId, asientoId, sesion, vence) > 0;
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
}
