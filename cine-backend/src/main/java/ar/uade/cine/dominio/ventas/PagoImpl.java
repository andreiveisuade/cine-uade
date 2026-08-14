package ar.uade.cine.dominio.ventas;

import java.time.LocalDateTime;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * El id llega vacío al construir de alta y con valor al reconstruir desde el DAO —
 * mismo patrón que el resto del dominio.
 */
public class PagoImpl implements Pago {

    private int id;
    private int reservaId;
    private Dinero subtotal;
    private Integer promocionId;
    private Dinero descuento;
    private Dinero monto;
    private MedioPago medio;
    private LocalDateTime fecha;
    private String codigoAutorizacion;

    /** Pago nuevo: se cobra ahora, todavía no tiene id. */
    public PagoImpl(int reservaId, Dinero subtotal, Integer promocionId, Dinero descuento,
                    MedioPago medio, LocalDateTime fecha, String codigoAutorizacion) {
        this.reservaId = reservaId;
        this.subtotal = subtotal;
        this.promocionId = promocionId;
        this.descuento = descuento;
        this.monto = subtotal.menos(descuento);
        this.medio = medio;
        this.fecha = fecha;
        this.codigoAutorizacion = codigoAutorizacion;
    }

    /** Pago que viene de la base. */
    public PagoImpl(int id, int reservaId, Dinero subtotal, Integer promocionId, Dinero descuento,
                    MedioPago medio, LocalDateTime fecha, String codigoAutorizacion) {
        this(reservaId, subtotal, promocionId, descuento, medio, fecha, codigoAutorizacion);
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getReservaId() {
        return reservaId;
    }

    @Override
    public Dinero getSubtotal() {
        return subtotal;
    }

    @Override
    public Integer getPromocionId() {
        return promocionId;
    }

    @Override
    public Dinero getDescuento() {
        return descuento;
    }

    /** No se guarda por separado: es siempre subtotal menos descuento. */
    @Override
    public Dinero getMonto() {
        return monto;
    }

    @Override
    public MedioPago getMedio() {
        return medio;
    }

    @Override
    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String getCodigoAutorizacion() {
        return codigoAutorizacion;
    }

    @Override
    public String toString() {
        return "[" + id + "] reserva " + reservaId + " - $" + monto
                + (descuento.esCero() ? "" : " (desc. $" + descuento + ")") + " - " + medio
                + " - " + fecha
                + (codigoAutorizacion == null || codigoAutorizacion.isBlank() ? "" : " - aut. " + codigoAutorizacion);
    }
}
