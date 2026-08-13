package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Cliente;

/**
 * El contrato: qué operaciones existen. No dice dónde ni cómo se guardan los datos.
 * El cliente no inicia sesión, así que el email —no una contraseña— es lo que lo
 * identifica de una visita a otra: por eso buscarPorEmail, no solo buscarPorId.
 */
public interface ClienteDAO {

    void guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(int id);

    /** Para no dar de alta dos veces al mismo cliente. */
    Optional<Cliente> buscarPorEmail(String email);

    List<Cliente> listar();

    void eliminar(int id);
}
