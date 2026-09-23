package ar.uade.cine.dto.programaciones;

import java.util.List;

/**
 * El mismo pedido para previsualizar y para dar de alta. {@code diasSemana} ausente o vacío
 * es todos los días.
 */
public record PedidoProgramacionDTO(Integer peliculaId, Integer salaId, String desde, String hasta,
                                 String horaInicio, List<String> diasSemana, String idioma,
                                 String proyeccion, Double precio) {
}
