package ar.uade.cine.service.usuarios;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Email;
import ar.uade.cine.repository.usuarios.ClienteRepository;
import ar.uade.cine.repository.usuarios.EmpleadoRepository;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Clientes por email: el ingreso con Google (R28), su nombre (R23) y su baja (R26); Usuario valida los datos.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorClientes {

    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;

    // R28: Google ya probó el email, así que quien compró sin cuenta es este cliente y conserva sus reservas y su nombre.
    // Antes de crearlo se mira el personal: comparten el UNIQUE del email, y el panel es Swing con su clave.
    public Cliente ingresarConGoogle(String nombre, String email) {
        String valor = new Email(email).valor();
        if (empleadoRepository.existsByEmail(valor)) {
            log.warn("ingreso con google rechazado · email del personal");
            throw new ConflictoDeNegocio("Ese email es del personal del cine: entrá desde la aplicación de escritorio");
        }
        Optional<Cliente> existente = clienteRepository.findByEmail(valor);
        Cliente cliente = existente.orElseGet(() -> clienteRepository.save(new Cliente(nombre, valor)));
        log.info("cliente {} entra con google · {}", cliente.getId(), existente.isPresent() ? "existente" : "nuevo");
        return cliente;
    }

    // R26: la cuenta no se borra (sus ventas quedan); solo queda constancia en el log, por id.
    @Transactional(readOnly = true)
    public void darDeBaja(String email) {
        buscarPorEmail(email).ifPresent(cliente -> log.info("cliente {} se dio de baja", cliente.getId()));
    }

    // R23: el email no cambia: es el de Google, con el que entra.
    public Cliente renombrar(String email, String nombre) {
        Cliente cliente = clienteRepository.findByEmail(email).orElseThrow();
        cliente.renombrar(nombre);
        return cliente;
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
