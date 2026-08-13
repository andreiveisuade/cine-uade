package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.AdministradorCine;

/**
 * El contrato: qué operaciones existen. No dice dónde ni cómo se guardan los datos.
 */
public interface AdministradorDAO {

    void guardar(AdministradorCine administrador);

    Optional<AdministradorCine> buscarPorId(int id);

    /** El email es la credencial con la que inicia sesión. */
    Optional<AdministradorCine> buscarPorEmail(String email);

    List<AdministradorCine> listar();

    void eliminar(int id);
}
