package ar.uade.cine.model.candy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.MedioPago;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Una venta del candy. A diferencia de la reserva de butacas, no hay estados: en el
 * mostrador se paga en el acto, así que la compra ya nace cobrada y lleva encima con qué se
 * pagó. Por eso tampoco pasa por Pago, que existe para el circuito de reservar primero y
 * cobrar después.
 */
@Entity
@Table(name = "compra_candy")
public class CompraCandy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * Quién compró, o {@code null} si la venta fue de mostrador y nadie se identificó: pedir
     * el nombre para vender pochoclos no tiene sentido.
     */
    @Column(name = "cliente_id")
    private Integer clienteId;

    /**
     * La reserva a la que se le agregó esta compra, o {@code null} si fue de mostrador. Es el
     * <em>«¿desea agregar pochoclos?»</em> que aparece después de comprar la entrada por la
     * web: de ahí sale el cliente sin volver a pedírselo, permite retirar mostrando el mismo
     * QR de la entrada, y le da al arqueo cuánto vende el upsell contra el mostrador.
     */
    @Column(name = "reserva_id")
    private Integer reservaId;

    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    private MedioPago medio;

    /** Código del procesador. Vacío cuando se pagó en efectivo. */
    private String codigoAutorizacion;

    /**
     * Qué se llevó. Se fijan al vender y no cambian: en el mostrador se paga en el acto, así
     * que la compra nace cerrada. Es un agregado, igual que la reserva con sus entradas: los
     * ítems no existen sin su compra, y por eso van con cascade y orphanRemoval.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "compra_id", nullable = false)
    private List<ItemCompra> items = new ArrayList<>();

    protected CompraCandy() {
    }

    public CompraCandy(Integer clienteId, Integer reservaId, LocalDateTime fecha, MedioPago medio,
                       String codigoAutorizacion, List<ItemCompra> items) {
        this.clienteId = clienteId;
        this.reservaId = reservaId;
        this.fecha = fecha;
        this.medio = medio;
        this.codigoAutorizacion = codigoAutorizacion;
        this.items.addAll(items);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public Integer getReservaId() {
        return reservaId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public MedioPago getMedio() {
        return medio;
    }

    public String getCodigoAutorizacion() {
        return codigoAutorizacion;
    }

    /** Copia defensiva: nadie modifica la lista interna desde afuera. */
    public List<ItemCompra> getItems() {
        return new ArrayList<>(items);
    }

    /** Derivado de los items: no se guarda por separado. */
    public Dinero getTotal() {
        return Dinero.sumar(items.stream().map(ItemCompra::getSubtotal).toList());
    }

    @Override
    public String toString() {
        return "[" + id + "] cliente " + clienteId + " - " + items + " - $" + getTotal()
                + " - " + medio + " - " + fecha;
    }
}
