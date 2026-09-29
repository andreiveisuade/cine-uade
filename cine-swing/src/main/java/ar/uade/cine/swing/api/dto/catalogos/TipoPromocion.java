package ar.uade.cine.swing.api.dto.catalogos;

import ar.uade.cine.swing.api.dto.promociones.PedidoPromocion;

import java.util.List;

/** {@code campos}: los de {@link PedidoPromocion} que pide este tipo, con el nombre que tienen en el JSON. */
public record TipoPromocion(String nombre, List<String> campos) {
}
