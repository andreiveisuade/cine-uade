package ar.uade.cine.dto.candy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code ahorro} contra comprar los productos sueltos: lo imprime el ticket. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompraCandyVistaDTO(int id, Integer clienteId, Integer reservaId, String fecha,
                               String medio, String codigoAutorizacion,
                               List<ItemCompraVistaDTO> items, double total, double ahorro) {
}
