package ar.uade.cine.dto.informes;

import java.util.List;

import ar.uade.cine.dto.candy.CompraCandyVistaDTO;

public record ArqueoCandyVistaDTO(String fecha, double total, List<CompraCandyVistaDTO> compras) {
}
