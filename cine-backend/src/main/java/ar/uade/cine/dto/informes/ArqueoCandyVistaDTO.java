package ar.uade.cine.dto.informes;

import java.util.List;

import ar.uade.cine.dto.candy.CompraCandyVistaDTO;

// La caja del candy de un día (GET /api/candy/arqueo): el total y cada compra, aparte de boletería.
public record ArqueoCandyVistaDTO(String fecha, double total, List<CompraCandyVistaDTO> compras) {
}
