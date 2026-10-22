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
import ar.uade.cine.model.rechazos.SinPrueba;
import ar.uade.cine.model.ventas.CodigoDeAcceso;
import ar.uade.cine.repository.ventas.ReservaRepository;

// Registro con clave e identificación por email de clientes; Usuario valida los datos, el gestor el email.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorClientes {

    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ReservaRepository reservaRepository;
    private final PasswordEncoder claves;

    // Dato inválido se rechaza antes que el email ocupado, que se busca también entre empleados (UNIQUE compartido).
    // Empleado o cliente con cuenta, el mismo texto que da abrirCuenta: el alta no delata emails del personal.
    // Si el email es de quien reservó sin cuenta, la abre sobre ese cliente y conserva sus reservas, pero solo con
    // el código de una de ellas (R22): si no, quien conozca el email vería sus códigos en /mias.
    public Cliente registrar(String nombre, String email, String password, String codigoReserva) {
        Cliente nuevo = new Cliente(nombre, email);
        Contrasena clave = new Contrasena(password);
        if (empleadoRepository.existsByEmail(nuevo.getEmail())) {
            throw new ConflictoDeNegocio("Ya existe una cuenta con ese email");
        }
        Cliente cliente = clienteRepository.findByEmail(nuevo.getEmail()).orElse(nuevo);
        if (cliente != nuevo && !cliente.tieneCuenta() && !esDeSuReserva(codigoReserva, cliente)) {
            // Mismo texto si falta o es ajeno: no confirma qué códigos existen.
            throw new SinPrueba("Para abrir esta cuenta hace falta el código de una de sus entradas: "
                    + "está en el ticket de la compra");
        }
        cliente.abrirCuenta(claves.encode(clave.valor()));
        return clienteRepository.save(cliente);
    }

    private boolean esDeSuReserva(String codigo, Cliente cliente) {
        return reservaRepository.existsByCodigoAndCliente_Id(new CodigoDeAcceso(codigo).valor(), cliente.getId());
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
        return Email.paraBuscar(email).map(Email::valor).flatMap(clienteRepository::findByEmail);
    }
}
