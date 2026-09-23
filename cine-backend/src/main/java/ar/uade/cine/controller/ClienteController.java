package ar.uade.cine.controller;

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
import org.springframework.web.util.UriComponentsBuilder;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.controller.vistas.VistasUsuarios;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.usuarios.PedidoClienteDTO;
import ar.uade.cine.service.usuarios.GestorClientes;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Clientes", description = "El alta y la búsqueda de quien compra")
@RestController
public class ClienteController {

    private final GestorClientes clientes;
    private final VistasUsuarios vistas;

    public ClienteController(GestorClientes clientes, VistasUsuarios vistas) {
        this.clientes = clientes;
        this.vistas = vistas;
    }

    // Literal null y no 200 sin cuerpo: el res.json() del front reventaría.
    @Operation(summary = "Buscar un cliente por email. Si no está, devuelve null")
    @GetMapping(value = "/api/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> buscarPorEmail(@RequestParam(required = false) String email) {
        Optional<Cliente> cliente = email == null || email.isBlank()
                ? Optional.empty()
                : clientes.buscarPorEmail(email.trim());
        return ResponseEntity.ok(cliente.map(c -> (Object) vistas.cliente(c)).orElse("null"));
    }

    @Operation(summary = "Registrar un cliente")
    @PostMapping("/api/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ClienteVistaDTO> registrar(@Valid @RequestBody PedidoClienteDTO pedido) {
        ClienteVistaDTO cliente = vistas.cliente(clientes.registrar(pedido.nombre(), pedido.email()));
        // No hay GET por id: el cliente se busca por email.
        return Creado.en(UriComponentsBuilder.fromPath("/api/clientes")
                .queryParam("email", cliente.email()).encode().toUriString(), cliente);
    }
}
