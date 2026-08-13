package ar.uade.cine.api;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.usuarios.Empleado;

/**
 * Las personas, en la forma que espera el front. No necesita ningún gestor: un cliente y
 * un empleado ya traen encima todo lo que se muestra de ellos.
 */
public class VistasUsuarios {

    public record ClienteVista(int id, String nombre, String email) {
    }

    /** El empleado que devuelve el login: sin el hash de la contraseña. */
    public record EmpleadoVista(int id, String nombre, String email, String rol) {
    }

    public ClienteVista cliente(Cliente c) {
        return new ClienteVista(c.getId(), c.getNombre(), c.getEmail());
    }

    public EmpleadoVista empleado(Empleado e) {
        return new EmpleadoVista(e.getId(), e.getNombre(), e.getEmail(), e.getRol().name());
    }
}
