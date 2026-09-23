package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.dinero.Dinero;

public interface GeneradorTicketCandy {

    void emitir(CompraCandy compra, Cliente cliente, Dinero ahorro);
}
