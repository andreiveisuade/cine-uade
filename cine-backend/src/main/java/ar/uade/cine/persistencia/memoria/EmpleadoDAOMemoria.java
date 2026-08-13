package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Empleado;
import ar.uade.cine.persistencia.EmpleadoDAO;

/** Implementación en memoria: sirve para probar la lógica sin levantar MySQL. */
public class EmpleadoDAOMemoria implements EmpleadoDAO {

    private final Map<Integer, Empleado> empleados = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Empleado administrador) {
        administrador.setId(proximoId++);
        empleados.put(administrador.getId(), administrador);
    }

    @Override
    public Optional<Empleado> buscarPorId(int id) {
        return Optional.ofNullable(empleados.get(id));
    }

    @Override
    public Optional<Empleado> buscarPorEmail(String email) {
        return empleados.values().stream()
                .filter(a -> a.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public List<Empleado> listar() {
        return new ArrayList<>(empleados.values());
    }

    @Override
    public void eliminar(int id) {
        empleados.remove(id);
    }
}
