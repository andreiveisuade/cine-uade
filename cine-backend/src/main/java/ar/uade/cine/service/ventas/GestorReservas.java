package ar.uade.cine.service.ventas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.comprobantes.GeneradorTicket;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.ButacaOcupada;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.usuarios.Cliente;
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
    private final GeneradorTicket generadorTicket;
    private final Ocupacion ocupacion;
    private final Reloj reloj;

    // El alta del cliente comparte la transacción: una reserva rechazada no lo deja creado. La función va
    // antes que el cliente: si no existe o ya empezó (R19), eso anula lo demás, y un email de empleado no
    // puede taparlo.
    public Reserva reservar(int funcionId, String nombre, String email, Map<String, TipoTarifa> butacas,
                            String sesion) {
        Funcion funcion = ocupacion.funcionEnVenta(funcionId);
        return vender(funcion, clientes.identificar(nombre, email), butacas, sesion);
    }

    public Reserva reservar(int funcionId, int clienteId, Map<String, TipoTarifa> butacas,
                            String sesion) {
        Funcion funcion = ocupacion.funcionEnVenta(funcionId);
        return vender(funcion, clienteRepository.exigir(clienteId, "el cliente"), butacas, sesion);
    }

    private Reserva vender(Funcion funcion, Cliente cliente, Map<String, TipoTarifa> butacas, String sesion) {
        // Un pedido sin butacas es uno vacío: la reserva sin entradas la rechaza Reserva.
        List<Entrada> entradas = armarEntradas(funcion, butacas == null ? Map.of() : butacas, sesion);
        Reserva reserva = guardarCompitiendoPorLasButacas(
                new Reserva(funcion, cliente, entradas, reloj.ahora()));
        if (sesion != null) {
            ocupacion.liberar(funcion.getId(), sesion);
        }
        log.info("reserva {} creada · funcion {} · {} · total {}", reserva.getId(), funcion.getId(),
                detalleDe(entradas), reserva.getTotal());

        generadorTicket.emitir(reserva);
        return reserva;
    }

    private List<Entrada> armarEntradas(Funcion funcion, Map<String, TipoTarifa> butacas, String sesion) {
        Sala sala = funcion.getSala();
        List<Asiento> deLaSala = asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(funcion.getSalaId());
        Set<Integer> ocupados = ocupacion.asientosOcupados(funcion.getId(), sesion);

        List<Entrada> entradas = new ArrayList<>();
        for (Map.Entry<String, TipoTarifa> pedido : butacas.entrySet()) {
            // Buscar entre los de esta sala garantiza que sea de la sala de la función; la base no lo valida.
            Asiento asiento = Asiento.exigirConCodigo(deLaSala, pedido.getKey());
            // La entrada valida su butaca (R9) y resuelve la tarifa; que esté libre depende de las otras reservas.
            Entrada entrada = new Entrada(asiento, pedido.getValue(), funcion.precioDe(asiento, sala));
            exigirLibre(asiento, ocupados);
            entradas.add(entrada);
        }
        return entradas;
    }

    private static void exigirLibre(Asiento asiento, Set<Integer> ocupados) {
        if (ocupados.contains(asiento.getId())) {
            throw new ButacaOcupada("La butaca " + asiento.getCodigo() + " ya está ocupada");
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
