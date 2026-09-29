package ar.uade.cine.swing.api.dto.usuarios;

public record Empleado(int id, String nombre, String email, String rol) {

    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(rol);
    }
}
