package ar.uade.cine.dto.grilla;

/**
 * Con qué restricciones armar la grilla de la semana.
 *
 * <p>El mismo pedido para previsualizar y para aplicar, igual que en las programaciones:
 * son la misma operación y solo se diferencian en si escriben. Que el informe fuera
 * distinto según el caso invitaría a que la cuenta también se separara.
 *
 * <p>Todo campo ausente toma su default en la ruta —una semana, de 14 a 24, ocho
 * títulos—: la pantalla que pide una grilla razonable no tiene que saber configurarla.
 */
public record PedidoGrillaDTO(String desde, Integer dias, String apertura, String cierre,
                              Integer cuantasPeliculas, Double precio, String idioma,
                              String proyeccion) {
}
