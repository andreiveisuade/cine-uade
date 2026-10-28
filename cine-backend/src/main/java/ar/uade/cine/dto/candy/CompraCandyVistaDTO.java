package ar.uade.cine.dto.candy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// Una venta de candy (/api/candy/compras, su arqueo y la reserva); omite lo nulo, como el medio de lo pendiente.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompraCandyVistaDTO(int id, Integer clienteId, Integer reservaId, String estado, String fecha,
                                  String cobradaEn, String retiradaEn, String medio, String codigoAutorizacion,
                                  List<ItemCompraVistaDTO> items, double total, double ahorro) {
}
