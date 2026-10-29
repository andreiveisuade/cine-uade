package ar.uade.cine.service.ventas;

import java.time.LocalDateTime;
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
import ar.uade.cine.model.rechazos.DatoInvalido;
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

    // Lo que costaría con los medios de la web (hoy solo Mercado Pago), sin abrir checkouts: el mismo cálculo que el
    // checkout (R30).
    @Transactional(readOnly = true)
    public List<Cotizacion> cotizar(int reservaId) {
        Reserva reserva = buscarReserva(reservaId);
        boolean soloCandy = validarLoQueSeCobra(reserva);
        Dinero candyPendiente = candyPendiente(reserva);
        return Arrays.stream(MedioPago.values())
                .filter(MedioPago::seCobraEnLaWeb)
                .map(medio -> cotizacion(reserva, medio, soloCandy, candyPendiente))
                .toList();
    }

    // R30: el candy sumado a una entrada ya paga se cobra sin las entradas y sin promo (D4: son solo de entradas).
    private Cotizacion cotizacion(Reserva reserva, MedioPago medio, boolean soloCandy, Dinero candyPendiente) {
        return soloCandy
                ? new Cotizacion(medio, Dinero.CERO, PoliticaPromociones.Descuento.ninguno(), candyPendiente)
                : new Cotizacion(medio, reserva.getTotal(), descuentoPara(reserva, medio), candyPendiente);
    }

    // Sin pagar se cobran entradas y candy (R5, R17, R19); pagada y con candy pendiente, solo el candy (R30).
    // Devuelve si es solo el candy. Pagada y sin pedido rechaza como siempre.
    private boolean validarLoQueSeCobra(Reserva reserva) {
        if (reserva.estaPagada() && candy.pendienteDe(reserva.getId()).isPresent()) {
            reserva.impedimentoParaPedirCandy(reloj.ahora()).ifPresent(motivo -> {
                throw motivo;
            });
            return true;
        }
        validarQueSePuedaCobrar(reserva);
        return false;
    }

    private Dinero candyPendiente(Reserva reserva) {
        return candy.pendienteDe(reserva.getId()).map(CompraCandy::getTotal).orElse(Dinero.CERO);
    }

    // Valida como al cobrar: mandar a pagar algo incobrable terminaría en una devolución, que no existe (R13).
    // Cada pedido abre un checkout nuevo; pagar dos abiertos queda acotado a la reserva y solo el primero la cobra (R5).
    public PasarelaPagos.Checkout iniciarCheckout(int reservaId, MedioPago medio) {
        Reserva reserva = buscarReserva(reservaId);
        boolean soloCandy = validarLoQueSeCobra(reserva);
        medio.exigirCheckout();

        Cotizacion cotizacion = cotizacion(reserva, medio, soloCandy, candyPendiente(reserva));

        PasarelaPagos.Checkout checkout = soloCandy
                ? pasarela.crearSoloCandy(reservaId, medio, cotizacion.candy(), venceElCandy(reserva))
                : pasarela.crear(reservaId, medio, cotizacion.entradasConDescuento(), cotizacion.candy(),
                        reserva.getCreadaEn().plusMinutes(Reserva.MINUTOS_PARA_PAGAR));
        log.info("checkout {} · reserva {} · {} · a pagar {}",
                checkout.id(), reservaId, medio, checkout.monto());
        return checkout;
    }

    // El mismo plazo que una reserva para pagar, y nunca después de que empieza la función (R30).
    private LocalDateTime venceElCandy(Reserva reserva) {
        LocalDateTime enMediaHora = reloj.ahora().plusMinutes(Reserva.MINUTOS_PARA_PAGAR);
        LocalDateTime inicio = reserva.getFuncion().getInicio();
        return enMediaHora.isBefore(inicio) ? enMediaHora : inicio;
    }

    // Se valida antes de autorizar, porque esa plata no tiene cómo volver (R13).
    // El de candy no devuelve un Pago: lo cobra la vuelta o la conciliación.
    public Pago confirmarCheckout(String checkoutId) {
        PasarelaPagos.Checkout checkout = buscarCheckout(checkoutId);
        if (checkout.soloCandy()) {
            throw new DatoInvalido("El checkout " + checkoutId
                    + " es del candy: se cobra cuando el comprador vuelve de la pasarela");
        }
        return cobrarCheckout(checkout, buscarReserva(checkout.reservaId()));
    }

    // El comprador vuelve al navegador sin nadie que lea JSON: lo que impide cobrar viaja como aviso.
    // El status de la back_url se escribe a mano: rejected no cobra, pero approved no alcanza, autoriza la pasarela.
    public RetornoCheckout retornarDeCheckout(String checkoutId, String status) {
        PasarelaPagos.Checkout checkout = buscarCheckout(checkoutId);
        Reserva reserva = buscarReserva(checkout.reservaId());
        if (!yaSeCobro(checkout, reserva)) {
            if ("rejected".equals(status)) {
                return new RetornoCheckout(reserva.getCodigo(),
                        "Mercado Pago rechazó el pago: probá de nuevo con otro medio");
            }
            try {
                cobrar(checkout, reserva);
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
        if (yaSeCobro(checkout, reserva)) {
            return false;
        }
        cobrar(checkout, reserva);
        return true;
    }

    // La idempotencia mira lo que cobra el checkout: la reserva PAGADA, o el pedido de candy que ya no está pendiente.
    private boolean yaSeCobro(PasarelaPagos.Checkout checkout, Reserva reserva) {
        return checkout.soloCandy() ? candy.pendienteDe(reserva.getId()).isEmpty() : reserva.estaPagada();
    }

    // Extract Method: volver y conciliar cobran igual y difieren solo en qué hacen con el rechazo.
    private void cobrar(PasarelaPagos.Checkout checkout, Reserva reserva) {
        if (checkout.soloCandy()) {
            cobrarCandyAparte(checkout, reserva);
        } else {
            cobrarCheckout(checkout, reserva);
        }
    }

    // La idempotencia mira el estado de la reserva (PAGADA en la misma transacción que el pago).
    private Pago cobrarCheckout(PasarelaPagos.Checkout checkout, Reserva reserva) {
        validarQueSePuedaCobrar(reserva);
        exigirElMismoPedido(checkout, reserva, false);
        return registrarPago(reserva, checkout.medio(), pasarela.autorizar(checkout));
    }

    // R30: sin Pago, así el borderó no cambia; el pedido toma el medio y la autorización del checkout.
    private void cobrarCandyAparte(PasarelaPagos.Checkout checkout, Reserva reserva) {
        if (!validarLoQueSeCobra(reserva)) {
            throw new ConflictoDeNegocio("La reserva " + reserva.getId()
                    + " no está pagada: el candy se cobra con ella");
        }
        exigirElMismoPedido(checkout, reserva, true);
        candy.cobrarPendiente(reserva.getId(), checkout.medio(), pasarela.autorizar(checkout), reloj.ahora());
    }

    // Se recalcula: si cambió una promo o el candy (R30), lo pagado ya no es lo que se cobraría.
    private void exigirElMismoPedido(PasarelaPagos.Checkout checkout, Reserva reserva, boolean soloCandy) {
        Cotizacion actual = cotizacion(reserva, checkout.medio(), soloCandy, candyPendiente(reserva));
        if (!checkout.monto().equals(actual.total())) {
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
