package ar.uade.cine.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.ItemCombo;
import ar.uade.cine.dominio.candy.ItemCompra;
import ar.uade.cine.dominio.candy.Producto;

/**
 * La carta del candy y sus ventas, en la forma que espera el front.
 *
 * <p>Un combo es un producto que además dice qué trae: por eso {@code componentes} viaja
 * en la misma forma que cualquier otro producto y no en un endpoint aparte.
 */
public class VistasCandy {

    /** Qué trae un combo: el producto que incluye y cuántas unidades. */
    public record ItemComboVista(int productoId, String nombre, int cantidad) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProductoVista(int id, String nombre, String tipo, double precio,
                                boolean disponible, boolean esCombo,
                                List<ItemComboVista> componentes) {
    }

    /** Una línea de la venta, con el precio que tenía el producto en ese momento. */
    public record ItemCompraVista(int productoId, String nombre, int cantidad,
                                  double precioUnitario, double subtotal) {
    }

    /**
     * {@code ahorro} es cuánto se ahorró por llevar combos en vez de los productos
     * sueltos: es el número que justifica el combo y el que el ticket imprime.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CompraCandyVista(int id, Integer clienteId, Integer reservaId, String fecha,
                                   String medio, String codigoAutorizacion,
                                   List<ItemCompraVista> items, double total, double ahorro) {
    }

    public ProductoVista producto(Producto p) {
        return new ProductoVista(p.getId(), p.getNombre(), p.getTipo().name(), p.getPrecio(),
                p.estaDisponible(), p.esCombo(),
                p.getComponentes().stream().map(this::componente).toList());
    }

    private ItemComboVista componente(ItemCombo c) {
        return new ItemComboVista(c.productoId(), c.nombre(), c.cantidad());
    }

    public CompraCandyVista compra(CompraCandy c, double ahorro) {
        return new CompraCandyVista(c.getId(), c.getClienteId(), c.getReservaId(),
                Fechas.texto(c.getFecha()), c.getMedio().name(), c.getCodigoAutorizacion(),
                c.getItems().stream().map(this::item).toList(), c.getTotal(), ahorro);
    }

    private ItemCompraVista item(ItemCompra i) {
        return new ItemCompraVista(i.productoId(), i.nombre(), i.cantidad(),
                i.precioUnitario(), i.getSubtotal());
    }
}
