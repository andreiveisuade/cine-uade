package ar.uade.cine.swing.api.dto;

public record Empleado(int id, String nombre, String email, String rol) {

    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(rol);
    }
}
