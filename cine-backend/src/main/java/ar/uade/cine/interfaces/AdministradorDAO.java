package ar.uade.cine.interfaces;

import java.util.List;
import java.util.Optional;

public interface AdministradorDAO {

    void guardar(AdministradorCine administrador);

    Optional<AdministradorCine> buscarPorId(int id);

    /** El email es la credencial con la que inicia sesión. */
    Optional<AdministradorCine> buscarPorEmail(String email);

    List<AdministradorCine> listar();

    void eliminar(int id);
}
