package ar.uade.cine.dto.candy;

import java.util.List;

public record ArqueoCandyVistaDTO(String fecha, double total, List<CompraCandyVistaDTO> compras) {
}
