package ar.uade.cine.service.ventas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.ButacaOcupada;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.ButacaPedida;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.salas.AsientoRepository;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.usuarios.GestorClientes;

// Vende y cancela butacas de una función (R4, R6, R9, R13, R19); coordina y cada entidad valida lo suyo.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorReservas {

    private final ReservaRepository reservaRepository;
    private final AsientoRepository asientoRepository;
    private final ClienteRepository clienteRepository;
    private final GestorClientes clientes;
    private final ApplicationEventPublisher eventos;
    private final FuncionesEnVenta enVenta;
    private final Ocupacion ocupacion;
    private final BloqueoDeButacas bloqueo;
    private final Reloj reloj;

    // El alta del cliente comparte la transacción: una reserva rechazada no lo deja creado. La función va
    // antes que el cliente: si no existe o ya empezó (R19), eso anula lo demás, y un email de empleado no
    // puede taparlo.
    public Reserva reservar(int funcionId, String nombre, String email, Map<String, TipoTarifa> butacas,
                            String sesion) {
        Funcion funcion = enVenta.exigir(funcionId);
        return vender(funcion, clientes.identificar(nombre, email), butacas, sesion);
    }

    public Reserva reservar(int funcionId, int clienteId, Map<String, TipoTarifa> butacas,
                            String sesion) {
        Funcion funcion = enVenta.exigir(funcionId);
        return vender(funcion, clienteRepository.exigir(clienteId, "el cliente"), butacas, sesion);
    }

    private Reserva vender(Funcion funcion, Cliente cliente, Map<String, TipoTarifa> butacas, String sesion) {
        // Un pedido sin butacas es uno vacío: la reserva sin entradas la rechaza Reserva.
        List<ButacaPedida> pedidas = butacasPedidas(funcion, butacas == null ? Map.of() : butacas);
        // La reserva crea sus entradas (Creador) y valida el pedido entero (tope, repetidas) antes de mirar la
        // ocupación, como el bloqueo: once butacas con una tomada eran un 409 y la web recargaba el mapa por un
        // pedido que nunca iba a pasar.
        Reserva nueva = new Reserva(funcion, cliente, pedidas, reloj.ahora());
        List<Entrada> entradas = nueva.getEntradas();
        exigirLibres(entradas, ocupacion.asientosOcupados(funcion.getId(), sesion));
        Reserva reserva = guardarCompitiendoPorLasButacas(nueva);
        if (sesion != null) {
            bloqueo.liberar(funcion.getId(), sesion);
        }
        log.info("reserva {} creada · funcion {} · {} · total {}", reserva.getId(), funcion.getId(),
                detalleDe(entradas), reserva.getTotal());

        // Observer: el ticket lo emite ComprobantesDeVentas cuando esta transacción confirma.
        eventos.publishEvent(new ReservaCreada(reserva.getId()));
        return reserva;
    }

    // Lo único que la reserva no puede hacer sola: buscar los asientos en la base. Lista y no mapa: "a1" y "A1"
    // son el mismo asiento, y un mapa los juntaría en silencio en vez de dejar que la reserva rechace la repetida.
    private List<ButacaPedida> butacasPedidas(Funcion funcion, Map<String, TipoTarifa> butacas) {
        List<Asiento> deLaSala = asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(funcion.getSalaId());

        List<ButacaPedida> pedidas = new ArrayList<>();
        for (Map.Entry<String, TipoTarifa> pedido : butacas.entrySet()) {
            // Buscar entre los de esta sala garantiza que sea de la sala de la función; la base no lo valida.
            pedidas.add(new ButacaPedida(Asiento.exigirConCodigo(deLaSala, pedido.getKey()), pedido.getValue()));
        }
        return pedidas;
    }

    private static void exigirLibres(List<Entrada> entradas, Set<Integer> ocupados) {
        for (Entrada entrada : entradas) {
            if (ocupados.contains(entrada.asientoId())) {
                throw new ButacaOcupada("La butaca " + entrada.codigoAsiento() + " ya está ocupada");
            }
        }
    }

    public Reserva cancelar(int reservaId) {
        Reserva reserva = reservaRepository.exigir(reservaId, "la reserva");
        reserva.cancelar();
        log.info("reserva {} CANCELADA · {} butacas vuelven a la venta",
                reservaId, reserva.getCantidadEntradas());
        return reserva;
    }

    private static String detalleDe(List<Entrada> entradas) {
        return entradas.stream()
                .map(e -> e.codigoAsiento() + "(" + e.tarifa() + ")")
                .collect(Collectors.joining(" "));
    }

    // saveAndFlush: la carrera por el UNIQUE (funcion_id, asiento_id) salta acá como 409 y no como 500 en el commit.
    private Reserva guardarCompitiendoPorLasButacas(Reserva reserva) {
        try {
            return reservaRepository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            throw new ButacaOcupada(
                    "Alguien tomó una de esas butacas mientras confirmabas la reserva", e);
        }
    }
}
