package ar.uade.cine.dto.usuarios;

// Quien está en sesión (POST /api/sesion), empleado o cliente, sin el hash; rol va por nombre de constante.
// empleado lo decide Rol: ningún cliente deduce del nombre del rol si entra al panel.
public record UsuarioVistaDTO(int id, String nombre, String email, String rol, boolean empleado) {
}
