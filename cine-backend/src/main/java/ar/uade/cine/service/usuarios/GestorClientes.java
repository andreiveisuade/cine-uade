package ar.uade.cine.service.usuarios;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.repository.ClienteRepository;
import ar.uade.cine.repository.CompraCandyRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.service.RecursoNoEncontrado;
import ar.uade.cine.service.ConflictoDeNegocio;

@Service
@Transactional
public class GestorClientes {

    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final CompraCandyRepository compraCandyRepository;

    public GestorClientes(ClienteRepository clienteRepository, ReservaRepository reservaRepository, CompraCandyRepository compraCandyRepository) {
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
        this.compraCandyRepository = compraCandyRepository;
    }

    public Cliente registrar(String nombre, String email) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (clienteRepository.findByEmail(email).isPresent()) {
            throw new ConflictoDeNegocio("Ya hay un cliente registrado con ese email");
        }
        Cliente cliente = new Cliente(nombre, email);
        clienteRepository.save(cliente);
        return cliente;
    }

    public Cliente identificar(String nombre, String email) {
        String buscado = email == null ? "" : email.trim();
        return buscarPorEmail(buscado).orElseGet(() -> registrar(nombre, buscado));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscar(int id) {
        return clienteRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    public void eliminar(int id) {
        if (!clienteRepository.existsById(id)) {
            throw new RecursoNoEncontrado("No existe el cliente " + id);
        }
        if (reservaRepository.existsByCliente_Id(id)) {
            throw new IllegalArgumentException("El cliente " + id + " tiene reservas: no se puede eliminar");
        }
        if (compraCandyRepository.existsByClienteId(id)) {
            throw new IllegalArgumentException(
                    "El cliente " + id + " tiene compras en el candy: no se puede eliminar");
        }
        clienteRepository.deleteById(id);
    }
}
