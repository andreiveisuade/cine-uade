package ar.uade.cine.service.ventas;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.repository.ventas.PagoRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.promociones.PoliticaPromociones;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

// Cobra reservas y su candy en caja o por checkout (R5, R11, R15, R17, R19, R30); coordina promos y pasarela.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorPagos {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final PoliticaPromociones promociones;
    private final PasarelaPagos pasarela;
    private final GestorCandy candy;
    private final ApplicationEventPublisher eventos;
    private final Reloj reloj;

    public Pago cobrar(int reservaId, MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = buscarReserva(reservaId);
        validarQueSePuedaCobrar(reserva);
        return registrarPago(reserva, medio, codigoAutorizacion);
    }

    // Sin save(): la reserva la trajo esta transacción y su cambio sale en el commit, donde se chequea @Version.
    // R30: el candy pendiente se cobra acá, con el mismo medio; el pago guarda solo las entradas (borderó, INCAA).
    private Pago registrarPago(Reserva reserva, MedioPago medio, String codigoAutorizacion) {
        PoliticaPromociones.Descuento descuento = descuentoPara(reserva, medio);

        Pago pago = new Pago(reserva.getId(), reserva.getTotal(),
                descuento.promocionId(), descuento.monto(),
                medio, reloj.ahora(), codigoAutorizacion);
        pagoRepository.save(pago);

        reserva.pagar();
        Optional<CompraCandy> conCandy = candy.cobrarPendiente(pago);
        // Observer: el recibo lo emite ComprobantesDeVentas cuando esta transacción confirma.
        eventos.publishEvent(new PagoRegistrado(pago.getId()));
        log.info("pago reserva {} · {} · subtotal {}{} · cobrado {}{}",
                reserva.getId(), medio, reserva.getTotal(),
                !descuento.monto().esCero()
                        ? " · promo " + descuento.promocionId() + " -" + descuento.monto()
                        : " · sin promo",
                pago.getMonto(), conCandy.map(c -> " · candy " + c.getTotal()).orElse(""));

        return pago;
    }

    // Lo que costaría con cada medio electrónico, sin abrir checkouts: el mismo cálculo que el checkout (R30).
    @Transactional(readOnly = true)
    public List<Cotizacion> cotizar(int reservaId) {
        Reserva reserva = buscarReserva(reservaId);
        validarQueSePuedaCobrar(reserva);
        Dinero candyPendiente = candyPendiente(reserva);
        return Arrays.stream(MedioPago.values())
                .filter(medio -> !medio.seCobraEnCaja())
                .map(medio -> cotizacion(reserva, medio, candyPendiente))
                .toList();
    }

    private Cotizacion cotizacion(Reserva reserva, MedioPago medio, Dinero candyPendiente) {
        return new Cotizacion(medio, reserva.getTotal(), descuentoPara(reserva, medio), candyPendiente);
    }

    private Dinero candyPendiente(Reserva reserva) {
        return candy.pendienteDe(reserva.getId()).map(CompraCandy::getTotal).orElse(Dinero.CERO);
    }

    // Valida como al cobrar: mandar a pagar algo incobrable terminaría en una devolución, que no existe (R13).
    // Cada pedido abre un checkout nuevo; pagar dos abiertos queda acotado a la reserva y solo el primero la cobra (R5).
    public PasarelaPagos.Checkout iniciarCheckout(int reservaId, MedioPago medio) {
        Reserva reserva = buscarReserva(reservaId);
        validarQueSePuedaCobrar(reserva);
        medio.exigirCheckout();

        Cotizacion cotizacion = cotizacion(reserva, medio, candyPendiente(reserva));

        PasarelaPagos.Checkout checkout = pasarela.crear(reservaId, medio, cotizacion.entradasConDescuento(),
                cotizacion.candy(), reserva.getCreadaEn().plusMinutes(Reserva.MINUTOS_PARA_PAGAR));
        log.info("checkout {} · reserva {} · {} · a pagar {}",
                checkout.id(), reservaId, medio, checkout.monto());
        return checkout;
    }

    // Se valida antes de autorizar, porque esa plata no tiene cómo volver (R13).
    public Pago confirmarCheckout(String checkoutId) {
        PasarelaPagos.Checkout checkout = buscarCheckout(checkoutId);
        return cobrarCheckout(checkout, buscarReserva(checkout.reservaId()));
    }

    // El comprador vuelve al navegador sin nadie que lea JSON: lo que impide cobrar viaja como aviso.
    // El status de la back_url se escribe a mano: rejected no cobra, pero approved no alcanza, autoriza la pasarela.
    public RetornoCheckout retornarDeCheckout(String checkoutId, String status) {
        PasarelaPagos.Checkout checkout = buscarCheckout(checkoutId);
        Reserva reserva = buscarReserva(checkout.reservaId());
        if (!reserva.estaPagada()) {
            if ("rejected".equals(status)) {
                return new RetornoCheckout(reserva.getCodigo(),
                        "Mercado Pago rechazó el pago: probá de nuevo con otro medio");
            }
            try {
                cobrarCheckout(checkout, reserva);
            } catch (Rechazo motivo) {
                return new RetornoCheckout(reserva.getCodigo(), motivo.getMessage());
            }
        }
        return new RetornoCheckout(reserva.getCodigo(), null);
    }

    // Conciliación: el mismo cobro que la vuelta, para quien pagó y no volvió; true si la cobró ahora.
    public boolean conciliarCheckout(String checkoutId) {
        PasarelaPagos.Checkout checkout = buscarCheckout(checkoutId);
        Reserva reserva = buscarReserva(checkout.reservaId());
        if (reserva.estaPagada()) {
            return false;
        }
        cobrarCheckout(checkout, reserva);
        return true;
    }

    // Extract Method: confirmar, volver y conciliar cobran igual y difieren solo en qué hacen con el rechazo.
    // La idempotencia mira el estado de la reserva (PAGADA en la misma transacción que el pago).
    private Pago cobrarCheckout(PasarelaPagos.Checkout checkout, Reserva reserva) {
        validarQueSePuedaCobrar(reserva);
        exigirElMismoPedido(checkout, reserva);
        return registrarPago(reserva, checkout.medio(), pasarela.autorizar(checkout));
    }

    // Se recalcula: si cambió una promo o el candy (R30), lo pagado ya no es lo que se cobraría.
    private void exigirElMismoPedido(PasarelaPagos.Checkout checkout, Reserva reserva) {
        if (!checkout.monto().equals(cotizacion(reserva, checkout.medio(), candyPendiente(reserva)).total())) {
            throw new ConflictoDeNegocio("El monto cambió después de abrir el pago: acercate a la boletería");
        }
    }

    private PasarelaPagos.Checkout buscarCheckout(String checkoutId) {
        return pasarela.buscar(checkoutId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe el checkout " + checkoutId));
    }

    private Reserva buscarReserva(int reservaId) {
        return reservaRepository.exigir(reservaId, "la reserva");
    }

    // Lo propio de la reserva lo decide ella, y del medio y la autorización, Pago; acá lo que necesita la base.
    private void validarQueSePuedaCobrar(Reserva reserva) {
        // R17: puede figurar RESERVADA si nadie consultó la función desde que venció.
        reserva.impedimentoParaCobrar(reloj.ahora()).ifPresent(motivo -> {
            throw motivo;
        });
    }

    // Recién al cobrar se conoce el medio, y de él dependen las promociones.
    private PoliticaPromociones.Descuento descuentoPara(Reserva reserva, MedioPago medio) {
        return promociones.calcularPara(reserva.getEntradas(), reserva.getFuncion().getInicio(), medio);
    }

    @Transactional(readOnly = true)
    public List<Pago> buscarPorReservas(Collection<Integer> reservaIds) {
        return pagoRepository.findByReservaIdIn(reservaIds);
    }

}
