package ar.uade.cine.model.ventas;

import ar.uade.cine.model.salas.Asiento;

// Una butaca del pedido con su tarifa: lo que la reserva necesita para crear la entrada (Creador).
public record ButacaPedida(Asiento asiento, TipoTarifa tarifa) {
}
