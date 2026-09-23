package ar.uade.cine.dto.grilla;

public record PedidoGrillaDTO(String desde, Integer dias, String apertura, String cierre,
                              Integer cuantasPeliculas, Double precio, String idioma,
                              String proyeccion) {
}
