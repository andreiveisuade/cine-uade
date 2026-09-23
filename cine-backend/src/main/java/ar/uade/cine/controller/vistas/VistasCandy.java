package ar.uade.cine.controller.vistas;

import org.springframework.stereotype.Component;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.dto.candy.CompraCandyVistaDTO;
import ar.uade.cine.dto.candy.ItemComboVistaDTO;
import ar.uade.cine.dto.candy.ItemCompraVistaDTO;
import ar.uade.cine.dto.candy.ProductoVistaDTO;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.candy.ItemCombo;
import ar.uade.cine.model.candy.ItemCompra;
import ar.uade.cine.model.candy.Producto;

@Component
public class VistasCandy {

    public ProductoVistaDTO producto(Producto p) {
        return new ProductoVistaDTO(p.getId(), p.getNombre(), p.getTipo().name(), p.getPrecio().aPesos(),
                p.estaDisponible(), p.esCombo(),
                p.getComponentes().stream().map(this::componente).toList());
    }

    private ItemComboVistaDTO componente(ItemCombo c) {
        return new ItemComboVistaDTO(c.producto().getId(), c.nombre(), c.cantidad());
    }

    public CompraCandyVistaDTO compra(CompraCandy c) {
        return new CompraCandyVistaDTO(c.getId(), c.getClienteId(), c.getReservaId(),
                Fechas.texto(c.getFecha()), c.getMedio().name(), c.getCodigoAutorizacion(),
                c.getItems().stream().map(this::item).toList(), c.getTotal().aPesos(), c.getAhorro().aPesos());
    }

    private ItemCompraVistaDTO item(ItemCompra i) {
        return new ItemCompraVistaDTO(i.producto().getId(), i.nombre(), i.cantidad(),
                i.precioUnitario().aPesos(), i.getSubtotal().aPesos());
    }
}
