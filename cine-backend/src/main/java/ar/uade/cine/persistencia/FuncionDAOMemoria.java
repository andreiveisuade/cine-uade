package ar.uade.cine.persistencia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.interfaces.Funcion;
import ar.uade.cine.interfaces.FuncionDAO;

public class FuncionDAOMemoria implements FuncionDAO {

    private final Map<Integer, Funcion> funciones = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Funcion funcion) {
        funcion.setId(proximoId++);
        funciones.put(funcion.getId(), funcion);
    }

    @Override
    public Optional<Funcion> buscarPorId(int id) {
        return Optional.ofNullable(funciones.get(id));
    }

    @Override
    public List<Funcion> listar() {
        return new ArrayList<>(funciones.values());
    }

    @Override
    public List<Funcion> listarPorPelicula(int peliculaId) {
        return funciones.values().stream().filter(f -> f.getPeliculaId() == peliculaId).toList();
    }

    @Override
    public List<Funcion> listarPorSala(int salaId) {
        return funciones.values().stream().filter(f -> f.getSalaId() == salaId).toList();
    }

    @Override
    public void eliminar(int id) {
        funciones.remove(id);
    }
}
