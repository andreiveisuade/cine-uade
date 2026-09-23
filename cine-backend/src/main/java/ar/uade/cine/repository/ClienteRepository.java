package ar.uade.cine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.usuarios.Cliente;

/** Ve solo clientes: tipado sobre la subclase, Hibernate filtra por el discriminador. */
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    /** El email identifica al cliente, que no inicia sesión. */
    Optional<Cliente> findByEmail(String email);
}
