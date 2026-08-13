package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.persistencia.SalaDAO;

/** Implementación en memoria: sirve para probar la lógica sin levantar MySQL. */
public class SalaDAOMemoria implements SalaDAO {

    private final Map<Integer, Sala> salas = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Sala sala) {
        sala.setId(proximoId++);
        salas.put(sala.getId(), sala);
    }

    @Override
    public Optional<Sala> buscarPorId(int id) {
        return Optional.ofNullable(salas.get(id));
    }

    @Override
    public List<Sala> listar() {
        return new ArrayList<>(salas.values());
    }

    @Override
    public void eliminar(int id) {
        salas.remove(id);
    }
}
