package ar.uade.cine.model.usuarios;

public enum Rol {

    CLIENTE,
    ADMINISTRADOR,

    ACOMODADOR;

    public boolean esEmpleado() {
        return this == ADMINISTRADOR || this == ACOMODADOR;
    }
}
