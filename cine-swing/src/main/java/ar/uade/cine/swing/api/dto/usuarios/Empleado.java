package ar.uade.cine.swing.api.dto.usuarios;

// El empleado que entró al panel; su rol decide si ve todo (encargado) o solo Puerta (acomodador).
public record Empleado(int id, String nombre, String email, String rol) {

    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(rol);
    }
}
