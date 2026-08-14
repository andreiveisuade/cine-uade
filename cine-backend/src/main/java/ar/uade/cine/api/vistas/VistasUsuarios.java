package ar.uade.cine.api.vistas;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.usuarios.Empleado;
import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.usuarios.EmpleadoVistaDTO;

/**
 * Las personas, en la forma que espera el front. No necesita ningún gestor: un cliente y
 * un empleado ya traen encima todo lo que se muestra de ellos.
 */
public class VistasUsuarios {

    public ClienteVistaDTO cliente(Cliente c) {
        return new ClienteVistaDTO(c.getId(), c.getNombre(), c.getEmail());
    }

    public EmpleadoVistaDTO empleado(Empleado e) {
        return new EmpleadoVistaDTO(e.getId(), e.getNombre(), e.getEmail(), e.getRol().name());
    }
}
