package ar.uade.cine.dto.candy;

import java.util.List;

/** El cierre del candy: la otra caja del cine, aparte de la boletería. */
public record ArqueoCandyVistaDTO(String fecha, double total, List<CompraCandyVistaDTO> compras) {
}
