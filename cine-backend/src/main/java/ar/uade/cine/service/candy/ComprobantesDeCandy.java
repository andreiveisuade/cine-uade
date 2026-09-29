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
//
// Patrón Observer (GoF), el mismo que ComprobantesDeVentas: GestorCandy (el sujeto) publica
// CompraCandyRegistrada y no sabe que existe un ticket; esta clase (el observador) lo emite.
// Qué resolvía: el ticket se escribía antes del commit, y una compra que después no se guardaba dejaba
// su ticket en la carpeta. Cómo se lee: @TransactionalEventListener corre recién cuando la transacción de
// la venta confirma (AFTER_COMMIT) y no corre si se deshace. Relee la compra por id en una transacción de
// solo lectura propia (REQUIRES_NEW), porque la de la venta ya terminó.
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
