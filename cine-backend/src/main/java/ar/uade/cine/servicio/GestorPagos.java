package ar.uade.cine.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Pago;
import ar.uade.cine.interfaces.PagoDAO;
import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.interfaces.ReservaDAO;
import ar.uade.cine.modelo.EstadoReserva;
import ar.uade.cine.modelo.MedioPago;
import ar.uade.cine.modelo.PagoImpl;

/**
 * Cobrar es un circuito aparte del de reservar: por eso tiene su propio gestor y no
 * vive dentro de GestorReservas, que ya tiene bastante con las butacas.
 */
public class GestorPagos {

    private final PagoDAO pagoDAO;
    private final ReservaDAO reservaDAO;

    public GestorPagos(PagoDAO pagoDAO, ReservaDAO reservaDAO) {
        this.pagoDAO = pagoDAO;
        this.reservaDAO = reservaDAO;
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
        if (medio == null) {
            throw new IllegalArgumentException("Falta el medio de pago");
        }
        if (medio.requiereAutorizacion() && (codigoAutorizacion == null || codigoAutorizacion.isBlank())) {
            throw new IllegalArgumentException("El pago con " + medio + " necesita código de autorización");
        }
        if (pagoDAO.buscarPorReserva(reservaId).isPresent()) {
            throw new IllegalArgumentException("La reserva " + reservaId + " ya tiene un pago registrado");
        }

        Pago pago = new PagoImpl(reservaId, reserva.getTotal(), medio, LocalDateTime.now(),
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
