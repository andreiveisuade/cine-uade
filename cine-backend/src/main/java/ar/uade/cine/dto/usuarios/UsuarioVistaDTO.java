package ar.uade.cine.dto.usuarios;

// Quien respondió al login (POST /api/sesion), empleado o cliente, sin el hash; rol va por nombre de constante.
public record UsuarioVistaDTO(int id, String nombre, String email, String rol) {
}
