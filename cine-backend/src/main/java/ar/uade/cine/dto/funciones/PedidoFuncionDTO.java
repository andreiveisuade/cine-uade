package ar.uade.cine.dto.funciones;

public record PedidoFuncionDTO(Integer peliculaId, Integer salaId, String inicio, String idioma,
                            String proyeccion, Double precio) {
}
