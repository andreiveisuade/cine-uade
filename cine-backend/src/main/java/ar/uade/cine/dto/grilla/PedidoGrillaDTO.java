package ar.uade.cine.dto.grilla;

/**
 * El mismo pedido para previsualizar y para aplicar. Todo campo ausente toma su default en la
 * ruta (una semana, de 14 a 24, ocho títulos).
 */
public record PedidoGrillaDTO(String desde, Integer dias, String apertura, String cierre,
                              Integer cuantasPeliculas, Double precio, String idioma,
                              String proyeccion) {
}
