package ar.uade.cine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.uade.cine.model.usuarios.Cliente;

/**
 * Los clientes.
 *
 * <p>Clientes y empleados comparten tabla, y este repositorio ve <strong>solo</strong> los
 * clientes: al estar tipado sobre la subclase, Hibernate le agrega el filtro por el
 * discriminador a cada consulta. Es lo que antes hacía a mano cada DAO con un
 * {@code WHERE rol = 'CLIENTE'} repetido en las cinco consultas.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    /** El email identifica al cliente: es con lo que se lo reconoce, porque no inicia sesión. */
    Optional<Cliente> findByEmail(String email);
}
