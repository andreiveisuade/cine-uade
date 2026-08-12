package ar.uade.cine.persistencia;

import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.interfaces.Asiento;
import ar.uade.cine.interfaces.AsientoDAO;

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
    public List<Asiento> listarPorSala(int salaId) {
        return asientos.stream().filter(a -> a.getSalaId() == salaId).toList();
    }
}
