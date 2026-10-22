package ar.uade.cine.dto.usuarios;

// Quien respondió al login (POST /api/sesion), empleado o cliente, sin el hash; rol va por nombre de constante.
// debeCambiarClave: el cliente entró con la clave temporal de boletería (R27) y la web le pide cambiarla.
public record UsuarioVistaDTO(int id, String nombre, String email, String rol, boolean debeCambiarClave) {
}
