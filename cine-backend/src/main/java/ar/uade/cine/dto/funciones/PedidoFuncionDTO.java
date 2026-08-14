package ar.uade.cine.dto.funciones;

/** El alta de una función suelta, la que carga el administrador fuera de una grilla. */
public record PedidoFuncionDTO(Integer peliculaId, Integer salaId, String inicio, String idioma,
                            String proyeccion, Double precio) {
}
