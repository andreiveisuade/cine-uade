package ar.uade.cine.dto.programaciones;

import java.util.List;

/**
 * El mismo pedido para previsualizar y para dar de alta: son la misma operación, y
 * mandarlos con formas distintas invitaría a que dejaran de serlo.
 *
 * <p>{@code diasSemana} ausente o vacío significa todos los días del rango, no
 * ninguno — igual que en las promociones.
 */
public record PedidoProgramacionDTO(Integer peliculaId, Integer salaId, String desde, String hasta,
                                 String horaInicio, List<String> diasSemana, String idioma,
                                 String proyeccion, Double precio) {
}
