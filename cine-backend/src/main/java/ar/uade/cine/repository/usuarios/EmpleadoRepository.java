package ar.uade.cine.repository.usuarios;

import java.util.Optional;


import ar.uade.cine.model.usuarios.Empleado;
import ar.uade.cine.repository.Repositorio;

// Persistencia de empleados; Repository de Spring Data, por email para el login de Spring Security.
public interface EmpleadoRepository extends Repositorio<Empleado> {

    Optional<Empleado> findByEmail(String email);

    boolean existsByEmail(String email);
}
