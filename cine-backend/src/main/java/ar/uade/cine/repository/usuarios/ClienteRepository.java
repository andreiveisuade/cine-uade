package ar.uade.cine.repository.usuarios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.usuarios.Cliente;

// Persistencia de clientes; Repository de Spring Data, que los busca por email. No ve a los empleados.
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);
}
