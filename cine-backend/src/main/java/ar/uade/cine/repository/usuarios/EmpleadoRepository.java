package ar.uade.cine.repository.usuarios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.usuarios.Empleado;

// Persistencia de empleados; Repository de Spring Data, por email para el login de Spring Security.
public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    Optional<Empleado> findByEmail(String email);

    boolean existsByEmail(String email);
}
