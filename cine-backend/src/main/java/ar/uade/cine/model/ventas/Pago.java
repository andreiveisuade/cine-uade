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

/**
 * Comprobante de cobro de una reserva. Tiene entidad propia —y no es un campo más de la
 * reserva— porque se consulta por sí mismo: para un arqueo hay que poder listar los pagos de
 * un día sin pasar por las reservas.
 */
@Entity
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "reserva_id", unique = true)
    private int reservaId;

    /**
     * Suma de los precios de lista de las entradas, antes de cualquier descuento.
     *
     * <p>No se pasa por parámetro: se toma del total de la reserva, así nadie cobra otra cosa.
     */
    private Dinero subtotal;

    /**
     * La promoción que ganó, o {@code null} si no aplicó ninguna. Guardarla además del monto
     * es lo que permite explicar meses después por qué se cobró eso, aunque para entonces la
     * promoción ya esté desactivada.
     */
    @Column(name = "promocion_id")
    private Integer promocionId;

    /** Cuánto sacó la promoción. Cero si no hubo. */
    private Dinero descuento;

    /** Lo que entró de verdad en la caja: subtotal menos descuento. Es lo que suma el arqueo. */
    private Dinero monto;

    @Enumerated(EnumType.STRING)
    private MedioPago medio;

    private LocalDateTime fecha;

    /** Código que devuelve el procesador. Vacío cuando se pagó en efectivo. */
    private String codigoAutorizacion;

    protected Pago() {
    }

    /** Pago nuevo: se cobra ahora, todavía no tiene id. */
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

    public void setId(int id) {
        this.id = id;
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

    /** No se guarda por separado: es siempre subtotal menos descuento. */
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
