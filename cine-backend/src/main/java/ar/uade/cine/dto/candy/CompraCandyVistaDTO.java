package ar.uade.cine.dto.candy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code ahorro} es cuánto se ahorró por llevar combos en vez de los productos
 * sueltos: es el número que justifica el combo y el que el ticket imprime.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompraCandyVistaDTO(int id, Integer clienteId, Integer reservaId, String fecha,
                               String medio, String codigoAutorizacion,
                               List<ItemCompraVistaDTO> items, double total, double ahorro) {
}
