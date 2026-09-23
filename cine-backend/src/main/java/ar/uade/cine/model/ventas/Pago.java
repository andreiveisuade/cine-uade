package ar.uade.cine.model.ventas;

import java.time.LocalDateTime;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "reserva_id", unique = true)
    private int reservaId;

    private Dinero subtotal;

    @Column(name = "promocion_id")
    private Integer promocionId;

    private Dinero descuento;

    private Dinero monto;

    @Enumerated(EnumType.STRING)
    private MedioPago medio;

    private LocalDateTime fecha;

    private String codigoAutorizacion;

    protected Pago() {
    }

    public Pago(int reservaId, Dinero subtotal, Integer promocionId, Dinero descuento,
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

    public int getId() {
        return id;
    }

    public int getReservaId() {
        return reservaId;
    }

    public Dinero getSubtotal() {
        return subtotal;
    }

    public Integer getPromocionId() {
        return promocionId;
    }

    public Dinero getDescuento() {
        return descuento;
    }

    public Dinero getMonto() {
        return monto;
    }

    public MedioPago getMedio() {
        return medio;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getCodigoAutorizacion() {
        return codigoAutorizacion;
    }
}
