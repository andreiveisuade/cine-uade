package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.AdministradorCine;
import ar.uade.cine.persistencia.AdministradorDAO;

public class AdministradorDAOMemoria implements AdministradorDAO {

    private final Map<Integer, AdministradorCine> administradores = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(AdministradorCine administrador) {
        administrador.setId(proximoId++);
        administradores.put(administrador.getId(), administrador);
    }

    @Override
    public Optional<AdministradorCine> buscarPorId(int id) {
        return Optional.ofNullable(administradores.get(id));
    }

    @Override
    public Optional<AdministradorCine> buscarPorEmail(String email) {
        return administradores.values().stream()
                .filter(a -> a.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public List<AdministradorCine> listar() {
        return new ArrayList<>(administradores.values());
    }

    @Override
    public void eliminar(int id) {
        administradores.remove(id);
    }
}
