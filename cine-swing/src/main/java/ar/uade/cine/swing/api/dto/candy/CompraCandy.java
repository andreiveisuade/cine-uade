package ar.uade.cine.swing.api.dto.candy;

import java.util.List;

// Una venta de candy como la leen Candy, Caja y la Puerta; sin reservaId fue de mostrador, ahorro es el de los combos.
// cobradaEn manda en los listados del día: el candy pedido en la web se pide (fecha) antes de cobrarse (R30).
public record CompraCandy(int id, Integer reservaId, String estado, String fecha, String cobradaEn,
                          String retiradaEn, String medio, String codigoAutorizacion, List<ItemCompra> items,
                          double total, double ahorro) {

    /** Cómo está para quien lo entrega: sin cobrar, a retirar o ya retirado. */
    public String situacion() {
        if (!"COBRADA".equals(estado)) return "Sin cobrar";
        return retiradaEn == null ? "A retirar" : "Retirado";
    }
}
