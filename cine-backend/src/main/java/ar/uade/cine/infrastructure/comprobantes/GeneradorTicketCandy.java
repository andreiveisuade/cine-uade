package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.usuarios.Cliente;

// Puerto: ticket de una compra de candy; ComprobantesDeCandy no conoce el formato (Variaciones protegidas).
public interface GeneradorTicketCandy {

    void emitir(CompraCandy compra, Cliente cliente);
}
