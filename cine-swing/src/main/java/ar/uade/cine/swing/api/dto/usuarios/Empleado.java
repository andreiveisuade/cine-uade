package ar.uade.cine.swing.api.dto.usuarios;

// El empleado que entró al panel; su rol decide si ve todo (encargado) o solo Puerta (acomodador).
// POST /api/sesion también acepta clientes: si entra al panel lo dice empleado, que decide el backend.
public record Empleado(int id, String nombre, String email, String rol, boolean empleado) {

    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(rol);
    }
}
