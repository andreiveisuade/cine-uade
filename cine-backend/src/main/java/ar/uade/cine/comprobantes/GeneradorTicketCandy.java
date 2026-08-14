package ar.uade.cine.comprobantes;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Emite el comprobante de una compra del candy. Va aparte de GeneradorTicket y no como
 * un segundo método suyo: quien emite tickets de butacas no tiene por qué saber de
 * pochoclos, y al revés.
 */
public interface GeneradorTicketCandy {

    /**
     * @param ahorro cuánto se ahorró el cliente por llevar combos en vez de los productos
     *               sueltos. Lo calcula el gestor, que es quien conoce la lista de precios.
     */
    void emitir(CompraCandy compra, Cliente cliente, Dinero ahorro);
}
