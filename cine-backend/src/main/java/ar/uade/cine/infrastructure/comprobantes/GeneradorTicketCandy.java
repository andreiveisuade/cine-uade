package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.usuarios.Cliente;

// Puerto del ticket de una compra de candy; GestorCandy no conoce el formato (Variaciones protegidas).
public interface GeneradorTicketCandy {

    void emitir(CompraCandy compra, Cliente cliente);
}
