package ar.uade.cine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.usuarios.Empleado;

/** Los empleados: administradores y acomodadores, los dos roles con contraseña. */
public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    Optional<Empleado> findByEmail(String email);
}
