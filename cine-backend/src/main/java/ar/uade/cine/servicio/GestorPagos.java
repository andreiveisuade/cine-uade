package ar.uade.cine.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.uade.cine.comprobantes.GeneradorRecibo;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.PagoImpl;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.pasarelas.PasarelaPagos;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.ReservaDAO;

/**
 * Cobrar es un circuito aparte del de reservar: por eso tiene su propio gestor y no
 * vive dentro de GestorReservas, que ya tiene bastante con las butacas.
 *
 * <p>Hay dos formas de cobrar y las dos terminan en el mismo {@link #cobrar}: el efectivo
 * en la boletería, donde el cajero registra la plata que recibió, y el medio electrónico,
 * que abre un checkout contra una {@link PasarelaPagos} y se confirma cuando el procesador
 * devuelve la autorización. Que sean el mismo método no es una economía de código: es lo
 * que garantiza que R5, R17 y R19 se apliquen igual por los dos caminos.
 */
public class GestorPagos {

    private static final Logger LOG = LoggerFactory.getLogger(GestorPagos.class);

    private final PagoDAO pagoDAO;
    private final ReservaDAO reservaDAO;
    private final FuncionDAO funcionDAO;
    private final PoliticaPromociones promociones;
    private final PasarelaPagos pasarela;
    private final GeneradorRecibo generadorRecibo;

    /**
     * Recibe la política de descuentos por contrato y no el gestor de promociones: cobrar
     * necesita un monto, no el ABM entero. Con la pasarela y el generador del recibo pasa
     * lo mismo: son contratos, así que este gestor no sabe si atrás hay MercadoPago o una
     * emulación, ni si el recibo sale en papel o en un archivo de texto.
     */
    public GestorPagos(PagoDAO pagoDAO, ReservaDAO reservaDAO, FuncionDAO funcionDAO,
                       PoliticaPromociones promociones, PasarelaPagos pasarela,
                       GeneradorRecibo generadorRecibo) {
        this.pagoDAO = pagoDAO;
        this.reservaDAO = reservaDAO;
        this.funcionDAO = funcionDAO;
        this.promociones = promociones;
        this.pasarela = pasarela;
        this.generadorRecibo = generadorRecibo;
    }

    /**
     * Registra el cobro y deja la reserva PAGADA. El monto no se recibe por parámetro:
     * sale del total de la reserva, así es imposible cobrar un importe distinto al que
     * corresponde por las butacas elegidas.
     */
    public Pago cobrar(int reservaId, MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = buscarReserva(reservaId);
        Funcion funcion = validarQueSePuedaCobrar(reserva);

        if (medio == null) {
            throw new IllegalArgumentException("Falta el medio de pago");
        }
        // R11: los medios electrónicos exigen código de autorización.
        if (medio.requiereAutorizacion() && (codigoAutorizacion == null || codigoAutorizacion.isBlank())) {
            throw new IllegalArgumentException("El pago con " + medio + " necesita código de autorización");
        }
        // R5, la otra mitad: y una sola vez.
        if (pagoDAO.buscarPorReserva(reservaId).isPresent()) {
            throw new IllegalArgumentException("La reserva " + reservaId + " ya tiene un pago registrado");
        }

        // Acá recién se sabe el medio de pago, y con él qué promociones corren: por eso
        // el total definitivo de una reserva no existe hasta que se cobra.
        PoliticaPromociones.Descuento descuento =
                promociones.calcularPara(reserva.getEntradas(), funcion.getInicio(), medio);

        Pago pago = new PagoImpl(reservaId, reserva.getTotal(),
                descuento.promocionId(), descuento.monto(),
                medio, LocalDateTime.now(),
                codigoAutorizacion == null ? "" : codigoAutorizacion.trim());
        pagoDAO.guardar(pago);

        reserva.setEstado(EstadoReserva.PAGADA);
        reservaDAO.actualizar(reserva);
        emitirRecibo(pago, reserva);
        // El desglose entero en una línea: subtotal, qué promoción entró y cuánto se
        // cobró al final. Es la pregunta que se hace en el arqueo cuando la caja no da,
        // y sin la promoción nombrada la diferencia no se puede explicar.
        LOG.info("pago reserva {} · {} · subtotal {}{} · cobrado {}",
                reservaId, medio, reserva.getTotal(),
                descuento.monto() > 0
                        ? " · promo " + descuento.promocionId() + " -" + descuento.monto()
                        : " · sin promo",
                pago.getMonto());

        return pago;
    }

    /**
     * Abre el checkout de la pasarela: el QR o el link que el cliente escanea para pagar.
     *
     * <p>Se valida todo lo que se valida al cobrar —R5, R17, R19— y no recién al confirmar,
     * porque mandar a alguien a pagar una reserva que no se puede cobrar termina en plata
     * cobrada que hay que devolver, y devolución es justamente lo que este sistema no
     * modela (R13).
     *
     * <p>El monto del checkout ya lleva el descuento aplicado. Tiene que ser así: el
     * cliente aprueba un importe concreto en la pantalla del procesador, y si después se
     * cobrara otro, lo aprobado y lo cobrado no coincidirían.
     */
    public PasarelaPagos.Checkout iniciarCheckout(int reservaId, MedioPago medio) {
        Reserva reserva = buscarReserva(reservaId);
        Funcion funcion = validarQueSePuedaCobrar(reserva);

        if (medio == null) {
            throw new IllegalArgumentException("Falta el medio de pago");
        }
        // R11 mirada al revés: el que no necesita autorización tampoco tiene a quién
        // pedírsela. El efectivo se cuenta en la caja del cine y se registra derecho con
        // cobrar, sin pasarela de por medio.
        if (!medio.requiereAutorizacion()) {
            throw new IllegalArgumentException("El pago con " + medio
                    + " se cobra en la caja del cine, no por checkout");
        }
        if (pagoDAO.buscarPorReserva(reservaId).isPresent()) {
            throw new IllegalArgumentException("La reserva " + reservaId + " ya tiene un pago registrado");
        }

        PoliticaPromociones.Descuento descuento =
                promociones.calcularPara(reserva.getEntradas(), funcion.getInicio(), medio);
        double monto = CalculadoraPrecio.redondear(reserva.getTotal() - descuento.monto());

        PasarelaPagos.Checkout checkout = pasarela.crear(reservaId, medio, monto);
        LOG.info("checkout {} · reserva {} · {} · a pagar {}",
                checkout.id(), reservaId, medio, monto);
        return checkout;
    }

    /**
     * El cliente pagó: la pasarela devuelve el código de autorización y con él se registra
     * el cobro. En una integración de verdad esto lo dispara el aviso del procesador; acá
     * lo dispara quien está atendiendo, que es la parte que la emulación no puede fingir
     * —alguien tiene que decir que la plata llegó—.
     *
     * <p>Qué se está pagando sale del checkout y no de quien confirma, por el mismo motivo
     * por el que el monto no viaja en el pedido de cobro: si la reserva y el medio fueran
     * datos de entrada, se podría autorizar un checkout de $16.000 y aplicarlo a otra
     * reserva.
     *
     * <p>El descuento se recalcula al cobrar y no se toma del checkout: entre que se abre y
     * se confirma pudo cambiar una promoción, y el mismo criterio ya rige en la grilla, que
     * revalida al aplicar en vez de confiar en lo previsualizado.
     */
    public Pago confirmarCheckout(String checkoutId) {
        PasarelaPagos.Checkout checkout = pasarela.buscar(checkoutId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el checkout " + checkoutId));

        String codigoAutorizacion = pasarela.autorizar(checkout);
        return cobrar(checkout.reservaId(), checkout.medio(), codigoAutorizacion);
    }

    private Reserva buscarReserva(int reservaId) {
        return reservaDAO.buscarPorId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva " + reservaId));
    }

    /**
     * Las tres reglas que decide el estado y el reloj, iguales para el efectivo y para el
     * checkout. Devuelve la función porque quien llama la necesita después: es de ella que
     * sale el horario contra el que se evalúan las promociones.
     */
    private Funcion validarQueSePuedaCobrar(Reserva reserva) {
        // R5: solo se cobra una reserva en estado RESERVADA.
        if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new IllegalArgumentException("La reserva está " + reserva.getEstado() + ", no se puede cobrar");
        }
        LocalDateTime ahora = LocalDateTime.now();
        // R17: puede seguir figurando RESERVADA porque nadie consultó esa función desde
        // que venció, y quien la expira es justamente la consulta. Chequearlo acá es lo
        // que impide cobrar butacas que ya volvieron a la venta.
        if (reserva.estaVencida(ahora)) {
            throw new IllegalArgumentException("La reserva " + reserva.getId()
                    + " venció: sus butacas volvieron a estar disponibles");
        }

        // La función se busca acá y no más abajo porque hacen falta dos cosas de ella: si
        // ya empezó (R19) y cuándo empieza, para saber qué promociones corren.
        Funcion funcion = funcionDAO.buscarPorId(reserva.getFuncionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la función " + reserva.getFuncionId()));
        // R19: una vez que la función arrancó, esa butaca ya no se vende. Es la misma
        // clase de validación temporal que R17 —el reloj invalidando algo que sigue
        // figurando disponible— y por eso van juntas.
        if (funcion.yaEmpezo(ahora)) {
            throw new IllegalArgumentException("La función ya empezó: no se puede cobrar la reserva "
                    + reserva.getId());
        }
        return funcion;
    }

    /**
     * El comprobante del cobro en efectivo. Los medios electrónicos no lo llevan: su
     * constancia es el cupón del procesador, que es de donde salió el código de
     * autorización que quedó guardado en el pago.
     *
     * <p>Va después de guardar y no antes porque el recibo se numera por el id del pago,
     * y hasta que el DAO no lo asigna, ese id no existe.
     */
    private void emitirRecibo(Pago pago, Reserva reserva) {
        if (!pago.getMedio().requiereAutorizacion()) {
            generadorRecibo.emitir(pago, reserva);
        }
    }

    public Optional<Pago> buscarPorReserva(int reservaId) {
        return pagoDAO.buscarPorReserva(reservaId);
    }

    public List<Pago> listarDelDia(LocalDate fecha) {
        return pagoDAO.listarPorFecha(fecha);
    }

    /** Arqueo: cuánto se cobró en el día. */
    public double totalCobrado(LocalDate fecha) {
        return CalculadoraPrecio.redondear(
                pagoDAO.listarPorFecha(fecha).stream().mapToDouble(Pago::getMonto).sum());
    }

    /**
     * El cierre de caja del día entero: el total, cuántas entradas se vendieron y cuánto
     * entró por cada medio de pago.
     *
     * <p>La cuenta vive acá y no en quien la muestra porque es la misma para la consola y
     * para la API, y porque cuánto se cobró es una pregunta del negocio. Se recorre una
     * sola vez la lista de cobros del día: los tres números salen de la misma pasada.
     */
    public Arqueo arqueoDe(LocalDate fecha) {
        List<Pago> delDia = pagoDAO.listarPorFecha(fecha);

        Map<MedioPago, Arqueo.TotalPorMedio> porMedio = new EnumMap<>(MedioPago.class);
        double total = 0;
        int entradas = 0;
        for (Pago pago : delDia) {
            Arqueo.TotalPorMedio acumulado = porMedio.getOrDefault(pago.getMedio(),
                    new Arqueo.TotalPorMedio(0, 0));
            porMedio.put(pago.getMedio(), new Arqueo.TotalPorMedio(acumulado.cantidad() + 1,
                    CalculadoraPrecio.redondear(acumulado.total() + pago.getMonto())));
            total += pago.getMonto();
            entradas += entradasDe(pago);
        }
        return new Arqueo(fecha, CalculadoraPrecio.redondear(total), entradas, porMedio, delDia);
    }

    /**
     * Cuántas butacas se llevó ese cobro. El pago no lo guarda —sería el mismo dato en dos
     * lados— así que se cuenta sobre las entradas de su reserva.
     */
    private int entradasDe(Pago pago) {
        return reservaDAO.buscarPorId(pago.getReservaId())
                .map(Reserva::getCantidadEntradas)
                .orElse(0);
    }

    public List<Pago> listar() {
        return pagoDAO.listar();
    }
}
