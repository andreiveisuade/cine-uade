package ar.uade.cine.service.usuarios;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.repository.ClienteRepository;
import ar.uade.cine.repository.CompraCandyRepository;
import ar.uade.cine.repository.ReservaRepository;

/**
 * Alta y baja de clientes. Depende de ReservaRepository y CompraCandyRepository además de ClienteRepository
 * solo para R12: antes de borrar hay que saber si el cliente tiene historial en cualquiera
 * de los dos circuitos de venta.
 */
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

    /** Devuelve el cliente ya con su id, para poder reservar a continuación. */
    public Cliente registrar(String nombre, String email) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (clienteRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya hay un cliente registrado con ese email");
        }
        Cliente cliente = new Cliente(nombre, email);
        clienteRepository.save(cliente);
        return cliente;
    }

    /**
     * Reconoce al cliente por su email y, si es la primera vez que compra, lo da de alta
     * en el momento.
     *
     * <p>Es una regla del negocio —comprar no exige registrarse antes— y no un atajo de
     * quien la llama: si la resolviera cada interfaz por su cuenta, una podría exigir el
     * registro previo y la otra no. Por eso el email se normaliza acá también: el que se
     * busca y el que se guarda tienen que ser el mismo, o el segundo intento de compra
     * daría de alta un cliente repetido.
     */
    public Cliente identificar(String nombre, String email) {
        String buscado = email == null ? "" : email.trim();
        return buscarPorEmail(buscado).orElseGet(() -> registrar(nombre, buscado));
    }

    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> buscar(int id) {
        return clienteRepository.findById(id);
    }

    /** El email identifica al cliente: es lo único que deja al comprar sin registrarse. */
    public Optional<Cliente> buscarPorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    /**
     * R12: un cliente con historial no se borra. Tanto <code>reserva</code> como
     * <code>compra_candy</code> lo referencian, así que borrarlo dejaría ventas huérfanas
     * —y la baja fallaría igual, pero con un error de SQL en vez de un mensaje entendible.
     */
    public void eliminar(int id) {
        if (clienteRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("No existe el cliente " + id);
        }
        if (!reservaRepository.findByClienteIdOrderByCreadaEnDesc(id).isEmpty()) {
            throw new IllegalArgumentException("El cliente " + id + " tiene reservas: no se puede eliminar");
        }
        if (!compraCandyRepository.findByClienteId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "El cliente " + id + " tiene compras en el candy: no se puede eliminar");
        }
        clienteRepository.deleteById(id);
    }
}
