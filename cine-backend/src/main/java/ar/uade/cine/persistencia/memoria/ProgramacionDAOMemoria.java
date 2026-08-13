package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.programaciones.Programacion;
import ar.uade.cine.persistencia.ProgramacionDAO;

/** Mismo contrato que la versión MySQL, en un mapa. Es la que usan los tests. */
public class ProgramacionDAOMemoria implements ProgramacionDAO {

    private final Map<Integer, Programacion> programaciones = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Programacion programacion) {
        programacion.setId(proximoId++);
        programaciones.put(programacion.getId(), programacion);
    }

    @Override
    public void actualizar(Programacion programacion) {
        programaciones.put(programacion.getId(), programacion);
    }

    @Override
    public Optional<Programacion> buscarPorId(int id) {
        return Optional.ofNullable(programaciones.get(id));
    }

    @Override
    public List<Programacion> listar() {
        return new ArrayList<>(programaciones.values());
    }

    @Override
    public List<Programacion> listarActivas() {
        return programaciones.values().stream().filter(Programacion::estaActiva).toList();
    }
}
