package ar.uade.cine.repository.usuarios;

import java.util.Optional;


import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.repository.Repositorio;

// Persistencia de clientes; Repository de Spring Data, que los busca por email. No ve a los empleados.
public interface ClienteRepository extends Repositorio<Cliente> {

    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);
}
