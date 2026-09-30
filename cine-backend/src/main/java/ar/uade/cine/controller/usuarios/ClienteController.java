package ar.uade.cine.controller.usuarios;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.usuarios.PedidoClienteDTO;
import ar.uade.cine.service.usuarios.GestorClientes;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/clientes: alta y búsqueda por email vía GestorClientes; sin GET por id, el email es la clave.
@Tag(name = "Clientes", description = "El alta y la búsqueda de quien compra")
@RestController
@RequiredArgsConstructor
public class ClienteController {

    private final GestorClientes clientes;
    private final VistasUsuarios vistas;

    // Un filtro sobre la colección, así que contesta una lista: con el cliente o vacía. Sin email,
    // vacía también: no se listan todos los clientes, el email es la clave.
    @Operation(summary = "Buscar un cliente por email: una lista con él, o vacía si no está")
    @GetMapping("/api/clientes")
    public List<ClienteVistaDTO> buscarPorEmail(@RequestParam(required = false) String email) {
        return clientes.buscarPorEmail(email).map(vistas::cliente).stream().toList();
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
