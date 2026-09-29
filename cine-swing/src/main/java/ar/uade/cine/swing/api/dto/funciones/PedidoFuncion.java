package ar.uade.cine.swing.api.dto.funciones;

// Campos objeto, igual que en el backend: lo que el encargado deja vacío viaja null y el mensaje lo da el gestor.
public record PedidoFuncion(Integer peliculaId, Integer salaId, String inicio, String idioma, String proyeccion,
                            Double precio) {
}
