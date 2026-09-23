package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.dinero.Dinero;

/** Emite el comprobante de una compra del candy, aparte del ticket de butacas. */
public interface GeneradorTicketCandy {

    /** @param ahorro combos contra productos sueltos; lo calcula el gestor, que tiene los precios */
    void emitir(CompraCandy compra, Cliente cliente, Dinero ahorro);
}
