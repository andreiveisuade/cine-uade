package ar.uade.cine.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.promociones.Promocion;
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
    private final GestorPromociones promociones;

    public GestorPagos(PagoDAO pagoDAO, ReservaDAO reservaDAO, FuncionDAO funcionDAO,
                       GestorPromociones promociones) {
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
        if (medio.requiereAutorizacion() && (codigoAutorizacion == null || codigoAutorizacion.isBlank())) {
            throw new IllegalArgumentException("El pago con " + medio + " necesita código de autorización");
        }
        if (pagoDAO.buscarPorReserva(reservaId).isPresent()) {
            throw new IllegalArgumentException("La reserva " + reservaId + " ya tiene un pago registrado");
        }

        // Acá recién se sabe el medio de pago, y con él qué promociones corren: por eso
        // el total definitivo de una reserva no existe hasta que se cobra.
        LocalDateTime inicioFuncion = funcionDAO.buscarPorId(reserva.getFuncionId())
                .map(Funcion::getInicio)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la función " + reserva.getFuncionId()));
        Optional<Promocion> ganadora = promociones.mejorPara(reserva.getEntradas(), inicioFuncion, medio);
        double descuento = ganadora.map(p -> promociones.descuentoDe(p, reserva.getEntradas())).orElse(0.0);

        Pago pago = new PagoImpl(reservaId, reserva.getTotal(),
                ganadora.map(Promocion::getId).orElse(null), descuento,
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
        return pagoDAO.listarPorFecha(fecha).stream().mapToDouble(Pago::getMonto).sum();
    }

    public List<Pago> listar() {
        return pagoDAO.listar();
    }
}
