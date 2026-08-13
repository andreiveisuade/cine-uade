package ar.uade.cine.dominio.ventas;

import java.time.LocalDateTime;

public class PagoImpl implements Pago {

    private int id;
    private int reservaId;
    private double subtotal;
    private Integer promocionId;
    private double descuento;
    private double monto;
    private MedioPago medio;
    private LocalDateTime fecha;
    private String codigoAutorizacion;

    public PagoImpl(int reservaId, double subtotal, Integer promocionId, double descuento,
                    MedioPago medio, LocalDateTime fecha, String codigoAutorizacion) {
        this.reservaId = reservaId;
        this.subtotal = subtotal;
        this.promocionId = promocionId;
        this.descuento = descuento;
        this.monto = subtotal - descuento;
        this.medio = medio;
        this.fecha = fecha;
        this.codigoAutorizacion = codigoAutorizacion;
    }

    public PagoImpl(int id, int reservaId, double subtotal, Integer promocionId, double descuento,
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
    public double getSubtotal() {
        return subtotal;
    }

    @Override
    public Integer getPromocionId() {
        return promocionId;
    }

    @Override
    public double getDescuento() {
        return descuento;
    }

    /** No se guarda por separado: es siempre subtotal menos descuento. */
    @Override
    public double getMonto() {
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
                + (descuento > 0 ? " (desc. $" + descuento + ")" : "") + " - " + medio
                + " - " + fecha
                + (codigoAutorizacion == null || codigoAutorizacion.isBlank() ? "" : " - aut. " + codigoAutorizacion);
    }
}
