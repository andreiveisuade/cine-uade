package ar.uade.cine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.usuarios.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByEmail(String email);
}
