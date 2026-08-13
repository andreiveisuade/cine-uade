package ar.uade.cine.persistencia;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.salas.Sala;

public interface SalaDAO {

    void guardar(Sala sala);

    Optional<Sala> buscarPorId(int id);

    List<Sala> listar();

    void eliminar(int id);
}
