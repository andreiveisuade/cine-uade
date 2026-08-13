package ar.uade.cine.persistencia.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.persistencia.ClienteDAO;

public class ClienteDAOMemoria implements ClienteDAO {

    private final Map<Integer, Cliente> clientes = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public void guardar(Cliente cliente) {
        cliente.setId(proximoId++);
        clientes.put(cliente.getId(), cliente);
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        return Optional.ofNullable(clientes.get(id));
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        return clientes.values().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public List<Cliente> listar() {
        return new ArrayList<>(clientes.values());
    }

    @Override
    public void eliminar(int id) {
        clientes.remove(id);
    }
}
