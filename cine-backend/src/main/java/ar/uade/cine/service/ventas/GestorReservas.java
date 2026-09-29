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
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.salas.AsientoRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.RecursoNoEncontrado;

// Vende y cancela butacas de una función (R4, R6, R9, R13, R19); coordina y cada entidad valida lo suyo.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorReservas {

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;
    private final AsientoRepository asientoRepository;
    private final GestorClientes clientes;
    private final GeneradorTicket generadorTicket;
    private final CalculadoraPrecio calculadoraPrecio;
    private final Ocupacion ocupacion;
    private final Reloj reloj;

    // El alta del cliente comparte la transacción: una reserva rechazada no lo deja creado.
    public Reserva reservar(int funcionId, String nombre, String email, Map<String, TipoTarifa> butacas,
                            String sesion) {
        Cliente cliente = clientes.identificar(nombre, email);
        return reservar(funcionId, cliente.getId(), butacas, sesion);
    }

    public Reserva reservar(int funcionId, int clienteId, Map<String, TipoTarifa> butacas,
                            String sesion) {
        Funcion funcion = buscarFuncion(funcionId);
        // R19: una función que ya arrancó no se vende; va primero porque anula las demás.
        if (funcion.yaEmpezo(reloj.ahora())) {
            throw new IllegalArgumentException("La función ya empezó: no se pueden reservar butacas");
        }
        Cliente cliente = clientes.buscar(clienteId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe el cliente " + clienteId));
        Sala sala = funcion.getSala();

        // Un pedido sin butacas es uno vacío: la reserva sin entradas la rechaza Reserva.
        List<Entrada> entradas = armarEntradas(funcion, sala, butacas == null ? Map.of() : butacas, sesion);
        Reserva reserva = guardarCompitiendoPorLasButacas(
                new Reserva(funcion, cliente, entradas, reloj.ahora()));
        if (sesion != null) {
            ocupacion.liberar(funcionId, sesion);
        }
        log.info("reserva {} creada · funcion {} · {} · total {}", reserva.getId(), funcionId,
                detalleDe(entradas), reserva.getTotal());

        generadorTicket.emitir(reserva);
        return reserva;
    }

    private List<Entrada> armarEntradas(Funcion funcion, Sala sala, Map<String, TipoTarifa> butacas,
                                        String sesion) {
        List<Asiento> deLaSala = asientoRepository.findBySala_IdOrderByFilaAscNumeroAsc(funcion.getSalaId());
        Set<Integer> ocupados = ocupacion.asientosOcupados(funcion.getId(), sesion);

        List<Entrada> entradas = new ArrayList<>();
        for (Map.Entry<String, TipoTarifa> pedido : butacas.entrySet()) {
            TipoTarifa tarifa = pedido.getValue() == null ? TipoTarifa.GENERAL : pedido.getValue();
            Asiento asiento = butacaVendible(deLaSala, pedido.getKey(), ocupados);
            entradas.add(new Entrada(asiento, tarifa,
                    calculadoraPrecio.precioDe(funcion, sala, asiento, tarifa)));
        }
        return entradas;
    }

    private static Asiento butacaVendible(List<Asiento> deLaSala, String codigo, Set<Integer> ocupados) {
        // Buscar entre los de esta sala garantiza que sea de la sala de la función; la base no lo valida.
        Asiento asiento = Asiento.exigirConCodigo(deLaSala, codigo);
        if (asiento.estaFueraDeServicio()) {
            throw new IllegalArgumentException("La butaca " + asiento.getCodigo() + " está fuera de servicio");
        }
        if (ocupados.contains(asiento.getId())) {
            throw new ButacaOcupadaException("La butaca " + asiento.getCodigo() + " ya está ocupada");
        }
        return asiento;
    }

    public Reserva cancelar(int reservaId) {
        Reserva reserva = buscarOFallar(reservaId);
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
            throw new ButacaOcupadaException(
                    "Alguien tomó una de esas butacas mientras confirmabas la reserva", e);
        }
    }

    private Funcion buscarFuncion(int funcionId) {
        return funcionRepository.findById(funcionId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la función " + funcionId));
    }

    private Reserva buscarOFallar(int id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la reserva " + id));
    }
}
