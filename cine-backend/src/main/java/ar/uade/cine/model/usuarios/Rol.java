package ar.uade.cine.model.usuarios;

/**
 * Discriminador de la tabla única de usuarios, y a la vez qué puede hacer cada uno.
 * CLIENTE es el que compra; los otros dos son {@link Empleado} y tienen contraseña.
 */
public enum Rol {

    CLIENTE,
    ADMINISTRADOR,

    /**
     * Valida entradas en la puerta y nada más. No administra la cartelera: por eso es un
     * rol propio y no un administrador con menos opciones de menú.
     */
    ACOMODADOR;

    /** Los dos roles con credenciales, que es lo que separa a un empleado de un cliente. */
    public boolean esEmpleado() {
        return this == ADMINISTRADOR || this == ACOMODADOR;
    }
}
