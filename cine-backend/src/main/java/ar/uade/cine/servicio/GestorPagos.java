package ar.uade.cine.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.PagoImpl;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.ReservaDAO;

/**
 * Cobrar es un circuito aparte del de reservar: por eso tiene su propio gestor y no
 * vive dentro de GestorReservas, que ya tiene bastante con las butacas.
 */
public class GestorPagos {

    private final PagoDAO pagoDAO;
    private final ReservaDAO reservaDAO;
    private final FuncionDAO funcionDAO;
    private final PoliticaPromociones promociones;

    /**
     * Recibe la política de descuentos por contrato y no el gestor de promociones: cobrar
     * necesita un monto, no el ABM entero.
     */
    public GestorPagos(PagoDAO pagoDAO, ReservaDAO reservaDAO, FuncionDAO funcionDAO,
                       PoliticaPromociones promociones) {
        this.pagoDAO = pagoDAO;
        this.reservaDAO = reservaDAO;
        this.funcionDAO = funcionDAO;
        this.promociones = promociones;
    }

    /**
     * Registra el cobro y deja la reserva PAGADA. El monto no se recibe por parámetro:
     * sale del total de la reserva, así es imposible cobrar un importe distinto al que
     * corresponde por las butacas elegidas.
     */
    public Pago cobrar(int reservaId, MedioPago medio, String codigoAutorizacion) {
        Reserva reserva = reservaDAO.buscarPorId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva " + reservaId));

        // R5: solo se cobra una reserva en estado RESERVADA.
        if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new IllegalArgumentException("La reserva está " + reserva.getEstado() + ", no se puede cobrar");
        }
        // R17: puede seguir figurando RESERVADA porque nadie consultó esa función desde
        // que venció, y quien la expira es justamente la consulta. Chequearlo acá es lo
        // que impide cobrar butacas que ya volvieron a la venta.
        if (reserva.estaVencida(LocalDateTime.now())) {
            throw new IllegalArgumentException("La reserva " + reservaId
                    + " venció: sus butacas volvieron a estar disponibles");
        }
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
        LocalDateTime inicioFuncion = funcionDAO.buscarPorId(reserva.getFuncionId())
                .map(Funcion::getInicio)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la función " + reserva.getFuncionId()));
        PoliticaPromociones.Descuento descuento =
                promociones.calcularPara(reserva.getEntradas(), inicioFuncion, medio);

        Pago pago = new PagoImpl(reservaId, reserva.getTotal(),
                descuento.promocionId(), descuento.monto(),
                medio, LocalDateTime.now(),
                codigoAutorizacion == null ? "" : codigoAutorizacion.trim());
        pagoDAO.guardar(pago);

        reserva.setEstado(EstadoReserva.PAGADA);
        reservaDAO.actualizar(reserva);

        return pago;
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
