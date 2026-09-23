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
 * Cobrar una reserva. Es un circuito aparte del de reservar y por eso tiene su gestor.
 *
 * <p>Hay dos formas de cobrar y las dos terminan en {@link #cobrar}: el efectivo en
 * boletería y el medio electrónico, que abre un checkout contra la {@link PasarelaPagos}
 * y se confirma con la autorización del procesador. Que sea el mismo método es lo que
 * garantiza que R5, R17 y R19 se apliquen igual por los dos caminos.
 *
 * <p>Recibe {@link PoliticaPromociones} y no el gestor de promociones: cobrar necesita un
 * monto, no el ABM entero. Con la pasarela y el recibo pasa lo mismo: son contratos.
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

    /**
     * Registra el cobro y deja la reserva PAGADA. El monto no se recibe por parámetro:
     * sale del total de la reserva, así es imposible cobrar un importe distinto al que
     * corresponde por las butacas elegidas.
     */
    public Pago cobrar(int reservaId, MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = buscarReserva(reservaId);
        Funcion funcion = validarQueSePuedaCobrar(reserva, medio);
        String autorizacion = medio.autorizacion(codigoAutorizacion);

        // Acá recién se sabe el medio de pago, y con él qué promociones corren: por eso
        // el total definitivo de una reserva no existe hasta que se cobra.
        PoliticaPromociones.Descuento descuento =
                promociones.calcularPara(reserva.getEntradas(), funcion.getInicio(), medio);

        Pago pago = new Pago(reservaId, reserva.getTotal(),
                descuento.promocionId(), descuento.monto(),
                medio, reloj.ahora(), autorizacion);
        pagoRepository.save(pago);

        reserva.pagar();
        reservaRepository.save(reserva);
        emitirRecibo(pago, reserva);
        // Con la promoción nombrada: es lo que explica la diferencia cuando la caja no da.
        LOG.info("pago reserva {} · {} · subtotal {}{} · cobrado {}",
                reservaId, medio, reserva.getTotal(),
                !descuento.monto().esCero()
                        ? " · promo " + descuento.promocionId() + " -" + descuento.monto()
                        : " · sin promo",
                pago.getMonto());

        return pago;
    }

    /**
     * Abre el checkout de la pasarela: el QR o link que el cliente escanea. Se valida lo
     * mismo que al cobrar y no recién al confirmar, porque mandar a pagar una reserva que
     * no se puede cobrar termina en una devolución, que el sistema no modela (R13). El
     * monto ya lleva el descuento: lo aprobado y lo cobrado tienen que coincidir.
     */
    public PasarelaPagos.Checkout iniciarCheckout(int reservaId, MedioPago medio) {
        Reserva reserva = buscarReserva(reservaId);
        Funcion funcion = validarQueSePuedaCobrar(reserva, medio);
        // R11 al revés: el efectivo se cuenta en la caja y se registra con cobrar, sin
        // pasarela de por medio.
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
     * El cliente pagó: la pasarela devuelve la autorización y con ella se cobra. Qué se
     * paga sale del checkout y no de quien confirma, si no se podría autorizar un checkout
     * de $16.000 y aplicarlo a otra reserva. El descuento se recalcula al cobrar: entre
     * abrir y confirmar pudo cambiar una promoción.
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
     * Lo que tiene que valer para cobrar, igual para el efectivo y para el checkout: el
     * estado de la reserva (R5), el reloj (R17, R19) y el medio. Devuelve la función
     * porque de ella sale el horario contra el que se evalúan las promociones.
     *
     * <p>El estado también lo exige {@code Reserva.pagar()}; acá se pregunta antes porque
     * el checkout no cobra todavía y no puede mandar a pagar algo que después se rechaza.
     */
    private Funcion validarQueSePuedaCobrar(Reserva reserva, MedioPago medio) {
        if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new IllegalArgumentException("La reserva está " + reserva.getEstado() + ", no se puede cobrar");
        }
        LocalDateTime ahora = reloj.ahora();
        // R17: puede seguir figurando RESERVADA porque nadie consultó esa función desde
        // que venció, y quien la expira es la consulta.
        if (reserva.estaVencida(ahora)) {
            throw new IllegalArgumentException("La reserva " + reserva.getId()
                    + " venció: sus butacas volvieron a estar disponibles");
        }
        Funcion funcion = funcionRepository.findById(reserva.getFuncionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la función " + reserva.getFuncionId()));
        // R19: una vez que la función arrancó, esa butaca ya no se vende.
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

    /**
     * El comprobante del efectivo; los medios electrónicos tienen el cupón del procesador.
     * Va después de guardar porque se numera por el id del pago.
     */
    private void emitirRecibo(Pago pago, Reserva reserva) {
        if (!pago.getMedio().requiereAutorizacion()) {
            generadorRecibo.emitir(pago, reserva);
        }
    }

    public Optional<Pago> buscarPorReserva(int reservaId) {
        return pagoRepository.findByReservaId(reservaId);
    }

    /** Los pagos de varias reservas de una vez, para que el listado no pida uno por fila. */
    public List<Pago> buscarPorReservas(Collection<Integer> reservaIds) {
        return pagoRepository.findByReservaIdIn(reservaIds);
    }

    public List<Pago> listar() {
        return pagoRepository.findAll();
    }
}
