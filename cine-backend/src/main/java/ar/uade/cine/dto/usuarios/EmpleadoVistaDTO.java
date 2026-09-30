package ar.uade.cine.dto.usuarios;

// El empleado que respondió al login (POST /api/sesion), sin el hash; rol va por nombre de constante.
public record EmpleadoVistaDTO(int id, String nombre, String email, String rol) {
}
