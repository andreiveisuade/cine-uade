package ar.uade.cine.service.usuarios;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Contrasena;
import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.usuarios.EmpleadoRepository;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Registro con clave e identificación por email de clientes; Usuario valida los datos, el gestor el email.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorClientes {

    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder claves;

    // Dato inválido se rechaza antes que el email ocupado, que se busca también entre empleados (UNIQUE compartido).
    // Si el email es de quien reservó sin cuenta, registrarse la abre sobre ese cliente y conserva sus reservas.
    public Cliente registrar(String nombre, String email, String password) {
        Cliente nuevo = new Cliente(nombre, email);
        Contrasena clave = new Contrasena(password);
        if (empleadoRepository.existsByEmail(nuevo.getEmail())) {
            throw new ConflictoDeNegocio("Ya existe un usuario con ese email");
        }
        Cliente cliente = clienteRepository.findByEmail(nuevo.getEmail()).orElse(nuevo);
        cliente.abrirCuenta(claves.encode(clave.valor()));
        return clienteRepository.save(cliente);
    }

    // Al comprar, el email de un empleado es 400 y no el 409 del alta, que la web toma como butaca perdida.
    public Cliente identificar(String nombre, String email) {
        return buscarPorEmail(email).orElseGet(() -> {
            Cliente nuevo = new Cliente(nombre, email);
            if (empleadoRepository.existsByEmail(nuevo.getEmail())) {
                throw new DatoInvalido(
                        "Ese email es de un empleado del cine: usá otro para comprar");
            }
            clienteRepository.save(nuevo);
            return nuevo;
        });
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    // Sin email no hay a quién buscar; con espacios o mayúsculas, se busca como lo guarda Email.
    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorEmail(String email) {
        return Email.paraBuscar(email).map(Email::valor).flatMap(clienteRepository::findByEmail);
    }
}
