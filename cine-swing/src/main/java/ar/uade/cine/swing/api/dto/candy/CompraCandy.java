package ar.uade.cine.swing.api.dto.candy;

import java.util.List;

// Una venta de candy como la leen Candy y Caja; sin reservaId fue de mostrador, ahorro es el de los combos.
public record CompraCandy(int id, Integer reservaId, String fecha, String medio,
                          String codigoAutorizacion, List<ItemCompra> items, double total, double ahorro) {
}
