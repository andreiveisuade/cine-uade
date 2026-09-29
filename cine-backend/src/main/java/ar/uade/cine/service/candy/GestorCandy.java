package ar.uade.cine.service.candy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.candy.CompraCandyRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.rechazos.DatoInvalido;

// Venta del candy en mostrador o para una reserva; Controlador: busca, CompraCandy valida y sale el ticket.
@Service
@Transactional
@RequiredArgsConstructor
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

    // El cliente sale de la reserva; si el pedido además nombra uno, tiene que ser ese: antes se lo
    // descartaba en silencio y la compra quedaba a nombre de otro sin que nadie se enterara.
    // Solo sobre una reserva pagada: el candy se retira con el QR de la entrada, y una cancelada o
    // vencida no tiene entrada que mostrar.
    public CompraCandy venderParaReserva(int reservaId, Integer clienteId, Map<Integer, Integer> cantidades,
                                         MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = reservaRepository.exigir(reservaId, "la reserva");
        if (clienteId != null && clienteId != reserva.getClienteId()) {
            throw new DatoInvalido("La reserva " + reservaId
                    + " es de otro cliente: revisá la reserva o el cliente");
        }
        if (!reserva.estaPagada()) {
            throw new DatoInvalido("La reserva " + reservaId
                    + " no está pagada: cobrala antes de agregarle candy");
        }
        return vender(reserva.getClienteId(), reservaId, cantidades, medio, codigoAutorizacion);
    }

    private CompraCandy vender(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades,
                               MedioPago medio, String codigoAutorizacion) {
        Cliente cliente = clienteId == null ? null : clienteRepository.exigir(clienteId, "el cliente");
        CompraCandy compra = new CompraCandy(clienteId, reservaId, reloj.ahora(), medio, codigoAutorizacion,
                productos.obtener(cantidades));
        compraCandyRepository.save(compra);
        // Observer: el ticket lo emite ComprobantesDeCandy cuando esta transacción confirma.
        eventos.publishEvent(new CompraCandyRegistrada(compra.getId()));
        return compra;
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
