package ar.uade.cine.swing.api.dto.funciones;

// Lo que Swing manda al programar una función suelta; R3 y R8 las valida el backend.
// Campos objeto, como en el backend: lo que el encargado deja vacío viaja null y el gestor da el mensaje.
public record PedidoFuncion(Integer peliculaId, Integer salaId, String inicio, String idioma, String proyeccion,
                            Double precio) {
}
