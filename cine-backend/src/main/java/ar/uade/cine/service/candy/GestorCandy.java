package ar.uade.cine.service.candy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.EstadoCompraCandy;
import ar.uade.cine.model.candy.ItemCompra;
import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.model.ventas.CodigoDeAcceso;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.candy.CompraCandyRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

// Venta del candy en mostrador, para una reserva o pedida en la web (R30); Controlador, CompraCandy valida.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorCandy {

    private final CompraCandyRepository compraCandyRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final ApplicationEventPublisher eventos;
    private final GestorProductos productos;
    private final Reloj reloj;

    public CompraCandy vender(Integer clienteId, Map<Integer, Integer> cantidades,
                              MedioPago medio, String codigoAutorizacion) {
        return vender(clienteId, null, cantidades, medio, codigoAutorizacion);
    }

    // El cliente sale de la reserva; si el pedido nombra uno, tiene que ser ese.
    // Solo reserva pagada: el candy se retira con el QR de la entrada.
    public CompraCandy venderParaReserva(int reservaId, Integer clienteId, Map<Integer, Integer> cantidades,
                                         MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = reservaRepository.exigir(reservaId, "la reserva");
        if (clienteId != null && clienteId != reserva.getClienteId()) {
            throw new DatoInvalido("La reserva " + reservaId
                    + " es de otro cliente: revisá la reserva o el cliente");
        }
        if (!reserva.estaPagada()) {
            throw new ConflictoDeNegocio("La reserva " + reservaId
                    + " no está pagada: cobrala antes de agregarle candy");
        }
        return vender(reserva.getClienteId(), reservaId, cantidades, medio, codigoAutorizacion);
    }

    private CompraCandy vender(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades,
                               MedioPago medio, String codigoAutorizacion) {
        // Solo para el 404: el cliente de la compra viaja por id, y el ticket lo busca el listener.
        if (clienteId != null) {
            clienteRepository.exigir(clienteId, "el cliente");
        }
        // Pedido y medio antes que los productos: efectivo con código se rechaza aunque falte un producto
        CompraCandy.validarPedido(cantidades, medio, codigoAutorizacion);
        CompraCandy compra = new CompraCandy(clienteId, reservaId, reloj.ahora(), medio, codigoAutorizacion,
                productos.obtener(cantidades));
        compraCandyRepository.save(compra);
        // Observer: el ticket lo emite ComprobantesDeCandy cuando esta transacción confirma.
        eventos.publishEvent(new CompraCandyRegistrada(compra.getId()));
        log.info("compra candy {} · {} · {} ítems · total ${} · {}", compra.getId(),
                reservaId != null ? "reserva " + reservaId : "mostrador",
                compra.getItems().stream().mapToInt(ItemCompra::cantidad).sum(), compra.getTotal(), medio);
        return compra;
    }

    // R30: reemplaza el pedido anterior y sin productos lo saca. Sin pagar se cobra con la reserva; ya pagada,
    // en un checkout aparte. Los precios se congelan ahora, como en el mostrador.
    public void pedirParaReserva(int reservaId, Map<Integer, Integer> cantidades) {
        Reserva reserva = reservaRepository.findParaCambiar(reservaId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la reserva " + reservaId));
        reserva.impedimentoParaPedirCandy(reloj.ahora()).ifPresent(motivo -> {
            throw motivo;
        });
        pendienteDe(reservaId).ifPresent(compraCandyRepository::delete);
        if (cantidades.isEmpty()) {
            log.info("candy de la reserva {} · sin pedido", reservaId);
            return;
        }
        CompraCandy pedido = compraCandyRepository.save(CompraCandy.pendiente(reserva.getClienteId(), reservaId,
                reloj.ahora(), productos.obtener(cantidades)));
        log.info("candy de la reserva {} · pedido {} · {} ítems · total ${}", reservaId, pedido.getId(),
                pedido.getItems().stream().mapToInt(ItemCompra::cantidad).sum(), pedido.getTotal());
    }

    public Optional<CompraCandy> pendienteDe(int reservaId) {
        return compraCandyRepository.findFirstByReservaIdAndEstado(reservaId, EstadoCompraCandy.PENDIENTE);
    }

    // R30: en la misma transacción que el pago de la reserva, con su medio, su autorización y su fecha.
    public Optional<CompraCandy> cobrarPendiente(Pago pago) {
        return cobrarPendiente(pago.getReservaId(), pago.getMedio(), pago.getCodigoAutorizacion(), pago.getFecha());
    }

    // R30: el de una entrada ya paga se cobra solo, con lo que autorizó su checkout.
    public Optional<CompraCandy> cobrarPendiente(int reservaId, MedioPago medio, String codigoAutorizacion,
                                                 LocalDateTime cuando) {
        Optional<CompraCandy> pendiente = pendienteDe(reservaId);
        pendiente.ifPresent(compra -> {
            compra.cobrar(medio, codigoAutorizacion, cuando);
            // Observer: el ticket lo emite ComprobantesDeCandy cuando esta transacción confirma.
            eventos.publishEvent(new CompraCandyRegistrada(compra.getId()));
            log.info("compra candy {} COBRADA · reserva {} · total ${} · {}", compra.getId(), reservaId,
                    compra.getTotal(), medio);
        });
        return pendiente;
    }

    // D7: con el QR de la entrada se entrega todo lo cobrado para esa reserva que todavía no se retiró.
    public List<CompraCandy> retirar(String codigo) {
        Reserva reserva = reservaRepository.exigirPorCodigo(new CodigoDeAcceso(codigo));
        List<CompraCandy> cobradas = compraCandyRepository.findByReservaIdIn(List.of(reserva.getId())).stream()
                .filter(CompraCandy::estaCobrada)
                .toList();
        if (cobradas.isEmpty()) {
            throw new ConflictoDeNegocio("La entrada no tiene candy para retirar");
        }
        List<CompraCandy> aRetirar = cobradas.stream().filter(CompraCandy::estaParaRetirar).toList();
        if (aRetirar.isEmpty()) {
            // Todo entregado: el rechazo lo arma la compra, con la hora en que se retiró.
            cobradas.get(cobradas.size() - 1).registrarRetiro(reloj.ahora());
        }
        aRetirar.forEach(compra -> compra.registrarRetiro(reloj.ahora()));
        log.info("candy retirado · reserva {} · {} compras", reserva.getId(), aRetirar.size());
        return aRetirar;
    }

    // CU-16: lo que el cliente compró; lo pendiente o anulado no es una compra y se ve en la reserva.
    @Transactional(readOnly = true)
    public List<CompraCandy> listarCobradasDe(String email) {
        return Email.paraBuscar(email).flatMap(clienteRepository::findByEmail)
                .map(cliente -> compraCandyRepository.findByClienteIdAndEstadoOrderByCobradaEnDesc(cliente.getId(),
                        EstadoCompraCandy.COBRADA))
                .orElse(List.of());
    }

    // R30: la reserva se canceló o venció, así que su pedido ya no se va a cobrar.
    public void anularPendiente(int reservaId) {
        pendienteDe(reservaId).ifPresent(compra -> {
            compra.anular();
            log.info("compra candy {} ANULADA · reserva {}", compra.getId(), reservaId);
        });
    }

    // Lo que figura en la reserva: lo anulado no se cobró ni se va a cobrar.
    @Transactional(readOnly = true)
    public List<CompraCandy> comprasDeReservas(Collection<Integer> reservaIds) {
        return compraCandyRepository.findByReservaIdIn(reservaIds).stream()
                .filter(compra -> compra.estaPendiente() || compra.estaCobrada())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompraCandy> listarComprasDelDia(LocalDate fecha) {
        return compraCandyRepository.findByDia(fecha);
    }

    @Transactional(readOnly = true)
    public List<CompraCandy> listarComprasDe(int clienteId) {
        return compraCandyRepository.findByClienteId(clienteId);
    }
}
