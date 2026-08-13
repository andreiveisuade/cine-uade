package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.persistencia.FuncionDAO;

/** Implementación en memoria: sirve para probar la lógica sin levantar MySQL. */
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
    public List<Funcion> listarPorProgramacion(int programacionId) {
        return funciones.values().stream()
                .filter(f -> f.getProgramacionId() != null && f.getProgramacionId() == programacionId)
                .toList();
    }

    @Override
    public void eliminar(int id) {
        funciones.remove(id);
    }
}
