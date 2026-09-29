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

// Emite ticket de reserva y recibo de cobro cuando ya quedaron guardados; Observer que corre tras el commit.
//
// Patrón Observer (GoF): el sujeto avisa que algo pasó y no sabe quién escucha ni qué hace con el aviso.
// Acá los sujetos son GestorReservas y GestorPagos, que publican ReservaCreada y PagoRegistrado con el
// ApplicationEventPublisher de Spring; esta clase es el observador. Spring hace de lista de suscriptores:
// encuentra los métodos anotados con @TransactionalEventListener por el tipo de su parámetro.
//
// Qué resolvía: los gestores escribían el archivo antes del commit. Si el commit fallaba después (el
// @Version de Reserva salta recién al hacer commit, cuando otra operación la canceló o la venció), quedaba
// en la carpeta un recibo de un cobro que no existió. Además cada gestor dependía de su generador.
//
// Cómo se lee: con la fase AFTER_COMMIT, que es la de @TransactionalEventListener por defecto, Spring
// guarda el aviso hasta que la transacción del gestor confirma, y si se deshace lo descarta: sin commit no
// hay comprobante. Para entonces la sesión de la venta ya no navega relaciones LAZY, así que cada método
// abre una transacción de solo lectura propia (REQUIRES_NEW, la única que Spring admite acá) y relee por id.
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

    // Después del commit la venta ya existe: si el archivo falla no hay nada que deshacer, y contestarle un
    // error al cliente lo haría reintentar una compra que ya hizo. Queda en el log para reimprimirlo.
    private static void emitir(Runnable comprobante, String cual) {
        try {
            comprobante.run();
        } catch (ComprobanteException e) {
            log.error("No se pudo emitir {}: la venta quedó registrada igual", cual, e);
        }
    }
}
