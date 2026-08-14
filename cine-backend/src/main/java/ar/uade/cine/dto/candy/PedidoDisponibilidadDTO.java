package ar.uade.cine.dto.candy;

/** Sacar un producto de la carta o reponerlo. No hay DELETE: viviría en compras viejas. */
public record PedidoDisponibilidadDTO(Boolean disponible) {
}
