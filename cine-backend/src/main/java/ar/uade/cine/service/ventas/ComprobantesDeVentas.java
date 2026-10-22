package ar.uade.cine.service.ventas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;
import ar.uade.cine.infrastructure.comprobantes.GeneradorRecibo;
import ar.uade.cine.infrastructure.comprobantes.GeneradorTicket;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ventas.PagoRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;

// Emite ticket de reserva y recibo de cobro cuando ya quedaron guardados.
// Patrón Observer (GoF): GestorReservas y GestorPagos publican ReservaCreada y PagoRegistrado; esta clase escucha.
// AFTER_COMMIT: sin commit no hay comprobante. REQUIRES_NEW de solo lectura: la sesión de la venta ya no navega LAZY.
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprobantesDeVentas {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final GeneradorTicket generadorTicket;
    private final GeneradorRecibo generadorRecibo;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void emitirTicket(ReservaCreada creada) {
        Reserva reserva = reservaRepository.exigir(creada.reservaId(), "la reserva");
        emitir(() -> generadorTicket.emitir(reserva), "el ticket de la reserva " + reserva.getId());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void emitirRecibo(PagoRegistrado registrado) {
        Pago pago = pagoRepository.exigir(registrado.pagoId(), "el pago");
        if (pago.llevaRecibo()) {
            Reserva reserva = reservaRepository.exigir(pago.getReservaId(), "la reserva");
            emitir(() -> generadorRecibo.emitir(pago, reserva), "el recibo del pago " + pago.getId());
        }
    }

    // Si el archivo falla no hay nada que deshacer: avisar error haría reintentar una compra ya hecha. Queda en el log.
    private static void emitir(Runnable comprobante, String cual) {
        try {
            comprobante.run();
        } catch (ComprobanteException e) {
            log.error("No se pudo emitir {}: la venta quedó registrada igual", cual, e);
        }
    }
}
