package ar.uade.cine.service.candy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.candy.CompraCandyRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.service.RecursoNoEncontrado;

// Venta del candy en mostrador o para una reserva; Controlador: busca, CompraCandy valida y sale el ticket.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorCandy {

    private final CompraCandyRepository compraCandyRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final GeneradorTicketCandy generadorTicket;
    private final GestorProductos productos;
    private final Reloj reloj;

    public CompraCandy vender(Integer clienteId, Map<Integer, Integer> cantidades,
                              MedioPago medio, String codigoAutorizacion) {
        return vender(clienteId, null, cantidades, medio, codigoAutorizacion);
    }

    public CompraCandy venderParaReserva(int reservaId, Map<Integer, Integer> cantidades,
                                         MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la reserva " + reservaId));
        return vender(reserva.getClienteId(), reservaId, cantidades, medio, codigoAutorizacion);
    }

    private CompraCandy vender(Integer clienteId, Integer reservaId, Map<Integer, Integer> cantidades,
                               MedioPago medio, String codigoAutorizacion) {
        Cliente cliente = clienteId == null ? null : clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe el cliente " + clienteId));
        CompraCandy compra = new CompraCandy(clienteId, reservaId, reloj.ahora(), medio, codigoAutorizacion,
                productos.buscarOFallar(cantidades));
        compraCandyRepository.save(compra);
        generadorTicket.emitir(compra, cliente);
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
