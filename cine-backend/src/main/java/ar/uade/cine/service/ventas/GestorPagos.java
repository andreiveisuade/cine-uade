package ar.uade.cine.service.ventas;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.comprobantes.GeneradorRecibo;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.ventas.EstadoReserva;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PagoRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.service.promociones.PoliticaPromociones;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.infrastructure.reloj.Reloj;

/**
 * Cobro de reservas. Efectivo y checkout por {@link PasarelaPagos} terminan en
 * {@link #cobrar}, así R5, R17 y R19 valen igual por los dos caminos. Recibe
 * {@link PoliticaPromociones} y no el gestor: cobrar necesita un monto, no el ABM.
 */
@Service
@Transactional
public class GestorPagos {

    private static final Logger LOG = LoggerFactory.getLogger(GestorPagos.class);

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;
    private final PoliticaPromociones promociones;
    private final PasarelaPagos pasarela;
    private final GeneradorRecibo generadorRecibo;
    private final Reloj reloj;

    public GestorPagos(PagoRepository pagoRepository, ReservaRepository reservaRepository, FuncionRepository funcionRepository,
                       PoliticaPromociones promociones, PasarelaPagos pasarela,
                       GeneradorRecibo generadorRecibo, Reloj reloj) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.funcionRepository = funcionRepository;
        this.promociones = promociones;
        this.pasarela = pasarela;
        this.generadorRecibo = generadorRecibo;
        this.reloj = reloj;
    }

    /** El monto no es parámetro: sale de la reserva, así no se puede cobrar otro importe. */
    public Pago cobrar(int reservaId, MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = buscarReserva(reservaId);
        Funcion funcion = validarQueSePuedaCobrar(reserva, medio);
        String autorizacion = medio.autorizacion(codigoAutorizacion);

        // Recién acá se conoce el medio, y de él dependen las promociones.
        PoliticaPromociones.Descuento descuento =
                promociones.calcularPara(reserva.getEntradas(), funcion.getInicio(), medio);

        Pago pago = new Pago(reservaId, reserva.getTotal(),
                descuento.promocionId(), descuento.monto(),
                medio, reloj.ahora(), autorizacion);
        pagoRepository.save(pago);

        reserva.pagar();
        reservaRepository.save(reserva);
        emitirRecibo(pago, reserva);
        // Con la promo nombrada: explica la diferencia cuando la caja no da.
        LOG.info("pago reserva {} · {} · subtotal {}{} · cobrado {}",
                reservaId, medio, reserva.getTotal(),
                !descuento.monto().esCero()
                        ? " · promo " + descuento.promocionId() + " -" + descuento.monto()
                        : " · sin promo",
                pago.getMonto());

        return pago;
    }

    /**
     * Valida como al cobrar y no recién al confirmar: mandar a pagar algo incobrable
     * terminaría en una devolución, que el sistema no modela (R13). El monto ya lleva el
     * descuento para que lo aprobado coincida con lo cobrado.
     */
    public PasarelaPagos.Checkout iniciarCheckout(int reservaId, MedioPago medio) {
        Reserva reserva = buscarReserva(reservaId);
        Funcion funcion = validarQueSePuedaCobrar(reserva, medio);
        // R11: el efectivo va por cobrar, sin pasarela.
        if (!medio.requiereAutorizacion()) {
            throw new IllegalArgumentException("El pago con " + medio
                    + " se cobra en la caja del cine, no por checkout");
        }

        PoliticaPromociones.Descuento descuento =
                promociones.calcularPara(reserva.getEntradas(), funcion.getInicio(), medio);
        Dinero monto = reserva.getTotal().menos(descuento.monto());

        PasarelaPagos.Checkout checkout = pasarela.crear(reservaId, medio, monto);
        LOG.info("checkout {} · reserva {} · {} · a pagar {}",
                checkout.id(), reservaId, medio, monto);
        return checkout;
    }

    /**
     * La reserva sale del checkout y no de quien confirma, para que una autorización no se
     * aplique a otra reserva. El descuento se recalcula: pudo cambiar una promoción.
     */
    public Pago confirmarCheckout(String checkoutId) {
        PasarelaPagos.Checkout checkout = pasarela.buscar(checkoutId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el checkout " + checkoutId));

        String codigoAutorizacion = pasarela.autorizar(checkout);
        return cobrar(checkout.reservaId(), checkout.medio(), codigoAutorizacion);
    }

    private Reserva buscarReserva(int reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva " + reservaId));
    }

    /**
     * R5, R17 y R19. El estado también lo exige {@code Reserva.pagar()}, pero el checkout
     * necesita saberlo antes de cobrar. Devuelve la función para evaluar las promociones.
     */
    private Funcion validarQueSePuedaCobrar(Reserva reserva, MedioPago medio) {
        if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new IllegalArgumentException("La reserva está " + reserva.getEstado() + ", no se puede cobrar");
        }
        LocalDateTime ahora = reloj.ahora();
        // R17: puede figurar RESERVADA si nadie consultó la función desde que venció.
        if (reserva.estaVencida(ahora)) {
            throw new IllegalArgumentException("La reserva " + reserva.getId()
                    + " venció: sus butacas volvieron a estar disponibles");
        }
        Funcion funcion = funcionRepository.findById(reserva.getFuncionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la función " + reserva.getFuncionId()));
        // R19
        if (funcion.yaEmpezo(ahora)) {
            throw new IllegalArgumentException("La función ya empezó: no se puede cobrar la reserva "
                    + reserva.getId());
        }
        if (medio == null) {
            throw new IllegalArgumentException("Falta el medio de pago");
        }
        // R5, la otra mitad: una sola vez.
        if (pagoRepository.existsByReservaId(reserva.getId())) {
            throw new IllegalArgumentException("La reserva " + reserva.getId() + " ya tiene un pago registrado");
        }
        return funcion;
    }

    /** Solo efectivo: lo electrónico tiene el cupón del procesador. Se numera por id del pago. */
    private void emitirRecibo(Pago pago, Reserva reserva) {
        if (!pago.getMedio().requiereAutorizacion()) {
            generadorRecibo.emitir(pago, reserva);
        }
    }

    public Optional<Pago> buscarPorReserva(int reservaId) {
        return pagoRepository.findByReservaId(reservaId);
    }

    /** De una vez, para que el listado no consulte por fila. */
    public List<Pago> buscarPorReservas(Collection<Integer> reservaIds) {
        return pagoRepository.findByReservaIdIn(reservaIds);
    }

    public List<Pago> listar() {
        return pagoRepository.findAll();
    }
}
