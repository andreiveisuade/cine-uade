package ar.uade.cine.dto.cartelera;

/**
 * Traer cartelera nueva de TMDB, ahora.
 *
 * <p>Un solo campo, y opcional: cuántas páginas traer, de veinte títulos cada una. Ausente
 * es una. Es {@code Integer} y no {@code int} como todos los pedidos: un cero mandado y un
 * campo que no vino tienen que poder distinguirse, y quien decide qué hacer con el ausente
 * es el gestor.
 */
public record PedidoImportacionDTO(Integer paginas) {
}
