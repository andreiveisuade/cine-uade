package ar.uade.cine.service.usuarios;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.service.ConflictoDeNegocio;

// Registro e identificación de clientes por email; Usuario valida los datos y el gestor el email repetido.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorClientes {

    private final ClienteRepository clienteRepository;

    // Nombre y email los valida Usuario. Se construye antes de buscar el email repetido para que
    // un dato inválido se rechace primero, como cuando la validación estaba acá.
    public Cliente registrar(String nombre, String email) {
        Cliente cliente = new Cliente(nombre, email);
        if (clienteRepository.findByEmail(email).isPresent()) {
            throw new ConflictoDeNegocio("Ya hay un cliente registrado con ese email");
        }
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
}
