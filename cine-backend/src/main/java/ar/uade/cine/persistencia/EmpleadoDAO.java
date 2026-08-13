package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Empleado;

public interface EmpleadoDAO {

    void guardar(Empleado administrador);

    Optional<Empleado> buscarPorId(int id);

    /** El email es la credencial con la que inicia sesión. */
    Optional<Empleado> buscarPorEmail(String email);

    List<Empleado> listar();

    void eliminar(int id);
}
