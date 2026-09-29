package ar.uade.cine.service.usuarios;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.usuarios.EmpleadoRepository;
import ar.uade.cine.service.ConflictoDeNegocio;

// Registro e identificación de clientes por email; Usuario valida los datos y el gestor el email ocupado.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorClientes {

    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;

    // Nombre y email los valida Usuario. Se construye antes de buscar el email repetido para que
    // un dato inválido se rechace primero, como cuando la validación estaba acá. Se busca también
    // entre los empleados: comparten el UNIQUE del email y ClienteRepository no los ve.
    public Cliente registrar(String nombre, String email) {
        Cliente cliente = new Cliente(nombre, email);
        if (clienteRepository.existsByEmail(cliente.getEmail())) {
            throw new ConflictoDeNegocio("Ya hay un cliente registrado con ese email");
        }
        if (empleadoRepository.existsByEmail(cliente.getEmail())) {
            throw new ConflictoDeNegocio("Ese email es de un empleado del cine");
        }
        clienteRepository.save(cliente);
        return cliente;
    }

    public Cliente identificar(String nombre, String email) {
        return buscarPorEmail(email).orElseGet(() -> registrar(nombre, email));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscar(int id) {
        return clienteRepository.findById(id);
    }

    // Sin email no hay a quién buscar; con espacios de más, se busca como lo guardó Usuario.
    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return clienteRepository.findByEmail(email.trim());
    }
}
