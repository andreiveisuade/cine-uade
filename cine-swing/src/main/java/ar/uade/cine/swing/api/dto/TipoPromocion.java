package ar.uade.cine.swing.api.dto;

import java.util.List;

/** {@code campos}: los de {@link PedidoPromocion} que pide este tipo, con el nombre que tienen en el JSON. */
public record TipoPromocion(String nombre, List<String> campos) {
}
