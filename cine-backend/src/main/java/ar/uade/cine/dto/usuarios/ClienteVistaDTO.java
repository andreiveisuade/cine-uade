package ar.uade.cine.dto.usuarios;

// Un cliente de /api/clientes, también embebido en reservas y en los pagos del arqueo.
public record ClienteVistaDTO(int id, String nombre, String email) {
}
