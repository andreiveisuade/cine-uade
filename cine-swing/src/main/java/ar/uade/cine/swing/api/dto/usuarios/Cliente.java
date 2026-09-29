package ar.uade.cine.swing.api.dto.usuarios;

// Un cliente del cine, buscado por email en la venta de candy o embebido en reservas y pagos.
public record Cliente(int id, String nombre, String email) {
}
