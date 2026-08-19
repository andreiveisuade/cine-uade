package ar.uade.cine.controller.vistas;

import org.springframework.stereotype.Component;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.ItemCombo;
import ar.uade.cine.model.candy.ItemCompra;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.dto.candy.CompraCandyVistaDTO;
import ar.uade.cine.dto.candy.ItemComboVistaDTO;
import ar.uade.cine.dto.candy.ItemCompraVistaDTO;
import ar.uade.cine.dto.candy.ProductoVistaDTO;
import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.model.dinero.Dinero;

/**
 * La carta del candy y sus ventas, en la forma que espera el front.
 *
 * <p>Un combo es un producto que además dice qué trae: por eso {@code componentes} viaja
 * en la misma forma que cualquier otro producto y no en un endpoint aparte.
 */
@Component
public class VistasCandy {

    public ProductoVistaDTO producto(Producto p) {
        return new ProductoVistaDTO(p.getId(), p.getNombre(), p.getTipo().name(), p.getPrecio().aPesos(),
                p.estaDisponible(), p.esCombo(),
                p.getComponentes().stream().map(this::componente).toList());
    }

    private ItemComboVistaDTO componente(ItemCombo c) {
        return new ItemComboVistaDTO(c.productoId(), c.nombre(), c.cantidad());
    }

    public CompraCandyVistaDTO compra(CompraCandy c, Dinero ahorro) {
        return new CompraCandyVistaDTO(c.getId(), c.getClienteId(), c.getReservaId(),
                Fechas.texto(c.getFecha()), c.getMedio().name(), c.getCodigoAutorizacion(),
                c.getItems().stream().map(this::item).toList(), c.getTotal().aPesos(), ahorro.aPesos());
    }

    private ItemCompraVistaDTO item(ItemCompra i) {
        return new ItemCompraVistaDTO(i.productoId(), i.nombre(), i.cantidad(),
                i.precioUnitario().aPesos(), i.getSubtotal().aPesos());
    }
}
