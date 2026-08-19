package ar.uade.cine.api.controladores;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.api.vistas.VistasUsuarios;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.usuarios.PedidoClienteDTO;
import ar.uade.cine.servicio.usuarios.GestorClientes;

/**
 * Registro del cliente (CU-05). Es opcional: reservar también da de alta a quien compra
 * por primera vez. Son dos caminos al mismo alta, no un paso previo del otro.
 */
@RestController
public class ControladorClientes {

    private final GestorClientes clientes;
    private final VistasUsuarios vistas;

    public ControladorClientes(GestorClientes clientes, VistasUsuarios vistas) {
        this.clientes = clientes;
        this.vistas = vistas;
    }

    /**
     * El cuerpo se arma con {@link ResponseEntity} y no devolviendo el DTO porque cuando no
     * hay cliente la respuesta es el literal {@code null} y no un cuerpo vacío: devolver
     * {@code null} desde el método haría que Spring conteste 200 sin nada, y el
     * {@code res.json()} del front reventaría en vez de leer "no está". Es parte del
     * contrato: se usa para reconocer a alguien, no para verificar que exista, así que no
     * encontrarlo es una respuesta válida y no un error.
     */
    @GetMapping(value = "/api/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> buscarPorEmail(@RequestParam(required = false) String email) {
        Optional<Cliente> cliente = email == null || email.isBlank()
                ? Optional.empty()
                : clientes.buscarPorEmail(email.trim());
        return ResponseEntity.ok(cliente.map(c -> (Object) vistas.cliente(c)).orElse("null"));
    }

    @PostMapping("/api/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteVistaDTO registrar(@RequestBody PedidoClienteDTO pedido) {
        return vistas.cliente(clientes.registrar(pedido.nombre(), pedido.email()));
    }
}
