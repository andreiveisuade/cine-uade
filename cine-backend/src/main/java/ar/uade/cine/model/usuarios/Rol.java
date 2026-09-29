package ar.uade.cine.model.usuarios;

// Rol de un usuario; su nombre es el rol de Spring Security y esEmpleado marca a los que tienen login.
public enum Rol {

    CLIENTE,
    ADMINISTRADOR,

    ACOMODADOR;

    public boolean esEmpleado() {
        return this == ADMINISTRADOR || this == ACOMODADOR;
    }
}
