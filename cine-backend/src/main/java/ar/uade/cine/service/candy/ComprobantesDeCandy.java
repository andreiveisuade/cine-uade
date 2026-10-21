package ar.uade.cine.service.candy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;
import ar.uade.cine.infrastructure.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.repository.candy.CompraCandyRepository;
import ar.uade.cine.repository.usuarios.ClienteRepository;

// Emite el ticket de una compra de candy cuando ya quedó guardada; Observer que escucha a GestorCandy.
// Patrón Observer (GoF): el ticket se emite recién AFTER_COMMIT; antes una compra no guardada dejaba su ticket.
// Relee la compra por id en una transacción propia (REQUIRES_NEW), porque la de la venta ya terminó.
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprobantesDeCandy {

    private final CompraCandyRepository compraCandyRepository;
    private final ClienteRepository clienteRepository;
    private final GeneradorTicketCandy generadorTicket;

    // Si el archivo falla, la compra ya está cobrada: no hay nada que deshacer, queda en el log.
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void emitirTicket(CompraCandyRegistrada registrada) {
        CompraCandy compra = compraCandyRepository.exigir(registrada.compraId(), "la compra");
        Cliente cliente = compra.getClienteId() == null ? null
                : clienteRepository.exigir(compra.getClienteId(), "el cliente");
        try {
            generadorTicket.emitir(compra, cliente);
        } catch (ComprobanteException e) {
            log.error("No se pudo emitir el ticket de la compra {}: la venta quedó registrada igual",
                    compra.getId(), e);
        }
    }
}
