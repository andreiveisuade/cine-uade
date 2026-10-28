package ar.uade.cine.service.usuarios;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.usuarios.EmpleadoRepository;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Clientes por email: al comprar, al entrar con Google (R28) y su nombre (R23); Usuario valida los datos.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorClientes {

    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;

    // R28: Google ya probó el email, así que quien compró sin cuenta es este cliente y conserva sus reservas y su nombre.
    // Antes de crearlo se mira el personal: comparten el UNIQUE del email, y el panel es Swing con su clave.
    public Cliente ingresarConGoogle(String nombre, String email) {
        String valor = new Email(email).valor();
        if (empleadoRepository.existsByEmail(valor)) {
            throw new ConflictoDeNegocio("Ese email es del personal del cine: entrá desde la aplicación de escritorio");
        }
        return clienteRepository.findByEmail(valor).orElseGet(() -> clienteRepository.save(new Cliente(nombre, valor)));
    }

    // R23: el email no cambia: es el de Google, con el que entra.
    public Cliente renombrar(String email, String nombre) {
        Cliente cliente = clienteRepository.findByEmail(email).orElseThrow();
        cliente.renombrar(nombre);
        return cliente;
    }

    // Al comprar, el email de un empleado es 400 y no el 409 del alta, que la web toma como butaca perdida.
    // El texto no nombra al empleado: la compra es pública y serviría para listar emails del personal.
    public Cliente identificar(String nombre, String email) {
        return buscarPorEmail(email).orElseGet(() -> {
            Cliente nuevo = new Cliente(nombre, email);
            if (empleadoRepository.existsByEmail(nuevo.getEmail())) {
                throw new DatoInvalido("No se puede comprar con ese email: usá otro");
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
        return Email.paraBuscar(email).flatMap(clienteRepository::findByEmail);
    }
}
