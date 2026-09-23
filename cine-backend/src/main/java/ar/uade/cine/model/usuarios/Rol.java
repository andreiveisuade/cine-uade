package ar.uade.cine.model.usuarios;

/** Discriminador de usuarios y a la vez permiso. Los que no son CLIENTE son {@link Empleado}. */
public enum Rol {

    CLIENTE,
    ADMINISTRADOR,

    /** Solo valida entradas en la puerta (R18). */
    ACOMODADOR;

    public boolean esEmpleado() {
        return this == ADMINISTRADOR || this == ACOMODADOR;
    }
}
