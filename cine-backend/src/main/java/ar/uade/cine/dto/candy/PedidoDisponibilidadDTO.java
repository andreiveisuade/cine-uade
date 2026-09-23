package ar.uade.cine.dto.candy;

/** No hay DELETE de productos: quedan referenciados en compras viejas. */
public record PedidoDisponibilidadDTO(Boolean disponible) {
}
