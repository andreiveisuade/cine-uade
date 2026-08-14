package ar.uade.cine.dto.usuarios;

/** El empleado que devuelve el login: sin el hash de la contraseña. */
public record EmpleadoVistaDTO(int id, String nombre, String email, String rol) {
}
