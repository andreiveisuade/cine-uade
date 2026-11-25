package ar.uade.cine.dto.comentarios;

// Un comentario de /api/comentarios; del autor solo el nombre, porque la lista es pública.
public record ComentarioVistaDTO(int id, String autor, int puntaje, String texto, String fecha) {
}
