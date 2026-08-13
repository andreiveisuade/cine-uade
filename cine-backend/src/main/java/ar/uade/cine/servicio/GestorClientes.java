package ar.uade.cine.servicio;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.usuarios.ClienteImpl;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.ReservaDAO;

public class GestorClientes {

    private final ClienteDAO clienteDAO;
    private final ReservaDAO reservaDAO;
    private final CompraCandyDAO compraCandyDAO;

    public GestorClientes(ClienteDAO clienteDAO, ReservaDAO reservaDAO, CompraCandyDAO compraCandyDAO) {
        this.clienteDAO = clienteDAO;
        this.reservaDAO = reservaDAO;
        this.compraCandyDAO = compraCandyDAO;
    }

    /** Devuelve el cliente ya con su id, para poder reservar a continuación. */
    public Cliente registrar(String nombre, String email) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (clienteDAO.buscarPorEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya hay un cliente registrado con ese email");
        }
        Cliente cliente = new ClienteImpl(nombre, email);
        clienteDAO.guardar(cliente);
        return cliente;
    }

    public List<Cliente> listar() {
        return clienteDAO.listar();
    }

    public Optional<Cliente> buscar(int id) {
        return clienteDAO.buscarPorId(id);
    }

    /** El email identifica al cliente: es lo único que deja al comprar sin registrarse. */
    public Optional<Cliente> buscarPorEmail(String email) {
        return clienteDAO.buscarPorEmail(email);
    }

    /**
     * R12: un cliente con historial no se borra. Tanto <code>reserva</code> como
     * <code>compra_candy</code> lo referencian, así que borrarlo dejaría ventas huérfanas
     * —y la baja fallaría igual, pero con un error de SQL en vez de un mensaje entendible.
     */
    public void eliminar(int id) {
        if (clienteDAO.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe el cliente " + id);
        }
        if (!reservaDAO.listarPorCliente(id).isEmpty()) {
            throw new IllegalArgumentException("El cliente " + id + " tiene reservas: no se puede eliminar");
        }
        if (!compraCandyDAO.listarPorCliente(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "El cliente " + id + " tiene compras en el candy: no se puede eliminar");
        }
        clienteDAO.eliminar(id);
    }
}
