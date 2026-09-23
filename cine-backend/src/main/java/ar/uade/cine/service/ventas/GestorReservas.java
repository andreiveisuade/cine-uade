package ar.uade.cine.service.ventas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.comprobantes.GeneradorTicket;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.EstadoAsiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.AsientoRepository;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.RecursoNoEncontrado;

@Service
@Transactional
public class GestorReservas {

    private static final Logger LOG = LoggerFactory.getLogger(GestorReservas.class);

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;
    private final AsientoRepository asientoRepository;
    private final GestorClientes clientes;
    private final GeneradorTicket generadorTicket;
    private final CalculadoraPrecio calculadoraPrecio;
    private final Ocupacion ocupacion;
    private final Reloj reloj;

    public GestorReservas(ReservaRepository reservaRepository, FuncionRepository funcionRepository,
                          AsientoRepository asientoRepository, GestorClientes clientes,
                          GeneradorTicket generadorTicket, CalculadoraPrecio calculadoraPrecio,
                          Ocupacion ocupacion, Reloj reloj) {
        this.reservaRepository = reservaRepository;
        this.funcionRepository = funcionRepository;
        this.asientoRepository = asientoRepository;
        this.clientes = clientes;
        this.generadorTicket = generadorTicket;
        this.calculadoraPrecio = calculadoraPrecio;
        this.ocupacion = ocupacion;
        this.reloj = reloj;
    }

    public Reserva reservar(int funcionId, int clienteId, Map<String, TipoTarifa> butacas) {
        return reservar(funcionId, clienteId, butacas, null);
    }

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
        if (butacas == null || butacas.isEmpty()) {
            throw new IllegalArgumentException("Hay que elegir al menos una butaca");
        }
        Sala sala = funcion.getSala();

        List<Entrada> entradas = armarEntradas(funcion, sala, butacas, sesion);
        Reserva reserva = guardarCompitiendoPorLasButacas(
                new Reserva(funcion, cliente, entradas, reloj.ahora()));
        if (sesion != null) {
            ocupacion.liberar(funcionId, sesion);
        }
        LOG.info("reserva {} creada · funcion {} · {} · total {}", reserva.getId(), funcionId,
                detalleDe(entradas), reserva.getTotal());

        emitirTicket(reserva, funcion, sala, cliente);
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
        Asiento asiento = Asiento.conCodigo(deLaSala, codigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La butaca " + Asiento.normalizarCodigo(codigo) + " no existe en esa sala"));
        if (asiento.getEstado() == EstadoAsiento.FUERA_DE_SERVICIO) {
            throw new IllegalArgumentException("La butaca " + asiento.getCodigo() + " está fuera de servicio");
        }
        if (ocupados.contains(asiento.getId())) {
            throw new IllegalArgumentException("La butaca " + asiento.getCodigo() + " ya está ocupada");
        }
        return asiento;
    }

    private void emitirTicket(Reserva reserva, Funcion funcion, Sala sala, Cliente cliente) {
        generadorTicket.emitir(reserva, funcion, funcion.getPelicula(), sala, cliente);
    }

    public void cancelar(int reservaId) {
        Reserva reserva = buscarOFallar(reservaId);
        reserva.cancelar();
        reservaRepository.save(reserva);
        LOG.info("reserva {} CANCELADA · {} butacas vuelven a la venta",
                reservaId, reserva.getCantidadEntradas());
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
