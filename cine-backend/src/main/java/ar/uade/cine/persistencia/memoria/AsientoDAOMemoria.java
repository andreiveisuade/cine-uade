package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.persistencia.AsientoDAO;

public class AsientoDAOMemoria implements AsientoDAO {

    private final List<Asiento> asientos = new ArrayList<>();
    private int proximoId = 1;

    @Override
    public void guardarTodos(List<Asiento> nuevos) {
        for (Asiento asiento : nuevos) {
            asiento.setId(proximoId++);
            asientos.add(asiento);
        }
    }

    @Override
    public void actualizar(Asiento asiento) {
        // Los objetos son los mismos que devuelve listarPorSala: alcanza con mutar el estado.
    }

    @Override
    public List<Asiento> listarPorSala(int salaId) {
        return asientos.stream().filter(a -> a.getSalaId() == salaId).toList();
    }
}
