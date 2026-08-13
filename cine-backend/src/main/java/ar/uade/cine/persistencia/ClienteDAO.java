package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Cliente;

public interface ClienteDAO {

    void guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(int id);

    /** Para no dar de alta dos veces al mismo cliente. */
    Optional<Cliente> buscarPorEmail(String email);

    List<Cliente> listar();

    void eliminar(int id);
}
