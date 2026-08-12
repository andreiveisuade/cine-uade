package ar.uade.cine.interfaces;

import java.util.List;
import java.util.Optional;

public interface ClienteDAO {

    void guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(int id);

    /** Para no dar de alta dos veces al mismo cliente. */
    Optional<Cliente> buscarPorEmail(String email);

    List<Cliente> listar();

    void eliminar(int id);
}
