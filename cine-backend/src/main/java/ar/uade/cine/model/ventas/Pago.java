package ar.uade.cine.model.ventas;

import java.time.LocalDateTime;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.validacion.ValidadorPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

// Cobro de una reserva, uno solo por reserva (R5); congela subtotal, descuento y monto al cobrar.
@Entity
@Getter
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

    // La autorización se arma acá y no en el gestor: es parte de lo que el pago tiene que cumplir (R11).
    public Pago(int reservaId, Dinero subtotal, Integer promocionId, Dinero descuento,
                MedioPago medio, LocalDateTime fecha, String codigoAutorizacion) {
        ValidadorPago.validar(subtotal, descuento, medio);
        String autorizacion = medio.autorizacion(codigoAutorizacion);
        this.reservaId = reservaId;
        this.subtotal = subtotal;
        this.promocionId = promocionId;
        this.descuento = descuento;
        this.monto = subtotal.menos(descuento);
        this.medio = medio;
        this.fecha = fecha;
        this.codigoAutorizacion = autorizacion;
    }
}
