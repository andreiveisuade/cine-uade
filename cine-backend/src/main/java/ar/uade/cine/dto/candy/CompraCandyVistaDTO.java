package ar.uade.cine.dto.candy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// Una venta de candy (/api/candy/compras y su arqueo); omite clienteId y reservaId si son nulos.
// El código de autorización viaja siempre: sin autorización, vacío.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompraCandyVistaDTO(int id, Integer clienteId, Integer reservaId, String fecha,
                                  String medio, String codigoAutorizacion,
                                  List<ItemCompraVistaDTO> items, double total, double ahorro) {
}
