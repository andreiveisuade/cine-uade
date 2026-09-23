package ar.uade.cine.swing.api.dto;

import java.util.List;

public record CompraCandy(int id, Integer reservaId, String fecha, String medio,
                          String codigoAutorizacion, List<ItemCompra> items, double total, double ahorro) {
}
