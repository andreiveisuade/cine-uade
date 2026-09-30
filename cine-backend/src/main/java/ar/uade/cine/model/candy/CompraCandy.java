package ar.uade.cine.model.candy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.candy.validacion.ValidadorCompraCandy;
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
import lombok.Getter;

// Venta del candy que nace cobrada; Creador de sus ItemCompra y suma el total. Valida ValidadorCompraCandy.
@Entity
@Table(name = "compra_candy")
@Getter
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

    // Creador: la compra contiene sus renglones, así que los crea ella y cada ItemCompra valida
    // lo suyo. GestorCandy solo busca los productos y pone la fecha. Todo se valida antes de asignar.
    public CompraCandy(Integer clienteId, Integer reservaId, LocalDateTime fecha, MedioPago medio,
                       String codigoAutorizacion, Map<Producto, Integer> cantidades) {
        String autorizacion = validarPedido(cantidades, medio, codigoAutorizacion);
        List<ItemCompra> renglones = cantidades.entrySet().stream()
                .map(renglon -> new ItemCompra(renglon.getKey(), renglon.getValue()))
                .toList();
        this.clienteId = clienteId;
        this.reservaId = reservaId;
        this.fecha = fecha;
        this.medio = medio;
        this.codigoAutorizacion = autorizacion;
        this.items.addAll(renglones);
    }

    // Lo que se valida sin buscar los productos: GestorCandy lo llama antes de ir a la base, así un pedido
    // vacío o un medio mal cargado no quedan tapados por el 404 de un producto. Devuelve el código limpio.
    public static String validarPedido(Map<?, Integer> cantidades, MedioPago medio, String codigoAutorizacion) {
        ValidadorCompraCandy.exigirProductos(cantidades);
        return ValidadorCompraCandy.medio(medio).autorizacion(codigoAutorizacion);
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
