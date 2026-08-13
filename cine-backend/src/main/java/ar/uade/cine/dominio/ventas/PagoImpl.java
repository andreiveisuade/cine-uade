package ar.uade.cine.dominio.ventas;

import java.time.LocalDateTime;

public class PagoImpl implements Pago {

    private int id;
    private int reservaId;
    private double monto;
    private MedioPago medio;
    private LocalDateTime fecha;
    private String codigoAutorizacion;

    public PagoImpl(int reservaId, double monto, MedioPago medio, LocalDateTime fecha,
                    String codigoAutorizacion) {
        this.reservaId = reservaId;
        this.monto = monto;
        this.medio = medio;
        this.fecha = fecha;
        this.codigoAutorizacion = codigoAutorizacion;
    }

    public PagoImpl(int id, int reservaId, double monto, MedioPago medio, LocalDateTime fecha,
                    String codigoAutorizacion) {
        this(reservaId, monto, medio, fecha, codigoAutorizacion);
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
        return "[" + id + "] reserva " + reservaId + " - $" + monto + " - " + medio
                + " - " + fecha
                + (codigoAutorizacion == null || codigoAutorizacion.isBlank() ? "" : " - aut. " + codigoAutorizacion);
    }
}
