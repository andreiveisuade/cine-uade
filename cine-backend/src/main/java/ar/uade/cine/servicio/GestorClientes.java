package ar.uade.cine.servicio;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Cliente;
import ar.uade.cine.interfaces.ClienteDAO;
import ar.uade.cine.modelo.ClienteImpl;

public class GestorClientes {

    private final ClienteDAO dao;

    public GestorClientes(ClienteDAO dao) {
        this.dao = dao;
    }

    public void registrar(String nombre, String email) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (dao.buscarPorEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya hay un cliente registrado con ese email");
        }
        dao.guardar(new ClienteImpl(nombre, email));
    }

    public List<Cliente> listar() {
        return dao.listar();
    }

    public Optional<Cliente> buscar(int id) {
        return dao.buscarPorId(id);
    }

    public void eliminar(int id) {
        if (dao.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe el cliente " + id);
        }
        dao.eliminar(id);
    }
}
