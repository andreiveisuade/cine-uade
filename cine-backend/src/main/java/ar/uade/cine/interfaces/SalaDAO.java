package ar.uade.cine.interfaces;

import java.util.List;
import java.util.Optional;

public interface SalaDAO {

    void guardar(Sala sala);

    Optional<Sala> buscarPorId(int id);

    List<Sala> listar();

    void eliminar(int id);
}
