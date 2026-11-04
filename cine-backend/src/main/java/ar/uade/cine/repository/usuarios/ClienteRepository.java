package ar.uade.cine.repository.usuarios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.repository.Repositorio;

// Persistencia de clientes; Repository de Spring Data, que los busca por email. No ve a los empleados.
public interface ClienteRepository extends Repositorio<Cliente> {

    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);

    // locate y no like: un % o un _ tipeados se buscan como texto. Vacío, todos.
    @Query("""
            select c from Cliente c
            where :texto = '' or locate(:texto, lower(c.nombre)) > 0 or locate(:texto, lower(c.email)) > 0
            order by c.nombre, c.id""")
    List<Cliente> buscar(@Param("texto") String texto);
}
