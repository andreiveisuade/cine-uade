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

@Entity
@Table(name = "compra_candy")
public class CompraCandy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "cliente_id")
    private Integer clienteId;

    @Column(name = "reserva_id")
    private Integer reservaId;

    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    private MedioPago medio;

    private String codigoAutorizacion;

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

    public List<ItemCompra> getItems() {
        return new ArrayList<>(items);
    }

    public Dinero getTotal() {
        return Dinero.sumar(items.stream().map(ItemCompra::getSubtotal).toList());
    }

    public Dinero getAhorro() {
        return Dinero.sumar(items.stream().map(ItemCompra::getAhorro).toList());
    }

    @Override
    public String toString() {
        return "[" + id + "] cliente " + clienteId + " - " + items + " - $" + getTotal()
                + " - " + medio + " - " + fecha;
    }
}
