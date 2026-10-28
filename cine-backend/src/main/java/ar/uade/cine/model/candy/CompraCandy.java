package ar.uade.cine.model.candy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.candy.validacion.ValidadorCompraCandy;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
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

// Venta del candy, cobrada en mostrador o pendiente de su reserva (R30); Creador de sus ItemCompra.
// Contexto del patrón State: cobrar y anular le piden la transición a EstadoCompraCandy.
@Entity
@Table(name = "compra_candy")
@Getter
public class CompraCandy {

    private static final DateTimeFormatter DIA_Y_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

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

    @Enumerated(EnumType.STRING)
    private EstadoCompraCandy estado;

    // El arqueo cuenta por cobro: el pedido de la web se hace un día y se cobra cuando se paga la reserva.
    private LocalDateTime cobradaEn;

    // Fecha y no estado, como ingresada_en de la reserva: sin ella el mismo QR retiraba dos veces el combo.
    private LocalDateTime retiradaEn;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "compra_id", nullable = false)
    private List<ItemCompra> items = new ArrayList<>();

    protected CompraCandy() {
    }

    // Creador: crea sus ItemCompra, cada uno valida lo suyo; todo se valida antes de asignar
    public CompraCandy(Integer clienteId, Integer reservaId, LocalDateTime fecha, MedioPago medio,
                       String codigoAutorizacion, Map<Producto, Integer> cantidades) {
        String autorizacion = validarPedido(cantidades, medio, codigoAutorizacion);
        this.items.addAll(renglones(cantidades));
        this.clienteId = clienteId;
        this.reservaId = reservaId;
        this.fecha = fecha;
        this.medio = medio;
        this.codigoAutorizacion = autorizacion;
        this.estado = EstadoCompraCandy.COBRADA;
        this.cobradaEn = fecha;
    }

    // R30: el pedido de la web, sin medio hasta que se cobra con su reserva.
    public static CompraCandy pendiente(int clienteId, int reservaId, LocalDateTime fecha,
                                        Map<Producto, Integer> cantidades) {
        ValidadorCompraCandy.exigirProductos(cantidades);
        CompraCandy pedido = new CompraCandy();
        pedido.items.addAll(renglones(cantidades));
        pedido.clienteId = clienteId;
        pedido.reservaId = reservaId;
        pedido.fecha = fecha;
        pedido.codigoAutorizacion = "";
        pedido.estado = EstadoCompraCandy.PENDIENTE;
        return pedido;
    }

    private static List<ItemCompra> renglones(Map<Producto, Integer> cantidades) {
        return cantidades.entrySet().stream()
                .map(renglon -> new ItemCompra(renglon.getKey(), renglon.getValue()))
                .toList();
    }

    // R30: con el medio y la autorización del pago de la reserva. El momento lo pone el gestor.
    public void cobrar(MedioPago medio, String codigoAutorizacion, LocalDateTime cuando) {
        EstadoCompraCandy siguiente = estado.cobrar();
        String autorizacion = ValidadorCompraCandy.medio(medio).autorizacion(codigoAutorizacion);
        this.medio = medio;
        this.codigoAutorizacion = autorizacion;
        this.cobradaEn = cuando;
        this.estado = siguiente;
    }

    // R30: la reserva se canceló o venció, así que el pedido ya no se cobra.
    public void anular() {
        estado = estado.anular();
    }

    // Con el QR de la entrada. El momento lo pone el gestor: la compra no sabe qué hora es.
    public void registrarRetiro(LocalDateTime cuando) {
        EstadoCompraCandy siguiente = estado.retirar();
        if (retiradaEn != null) {
            throw new ConflictoDeNegocio("El candy de esa entrada ya se retiró el " + retiradaEn.format(DIA_Y_HORA));
        }
        estado = siguiente;
        retiradaEn = cuando;
    }

    public boolean estaParaRetirar() {
        return estado.estaCobrada() && retiradaEn == null;
    }

    public boolean estaPendiente() {
        return estado.esperaCobro();
    }

    public boolean estaCobrada() {
        return estado.estaCobrada();
    }

    // Valida sin buscar productos: así un pedido vacío no queda tapado por el 404 de un producto. Devuelve el código limpio
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
                + " - " + estado + " - " + medio + " - " + fecha;
    }
}
