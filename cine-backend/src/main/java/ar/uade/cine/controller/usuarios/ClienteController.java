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

    // Filtro sobre la colección: lista con el cliente o vacía; sin email también vacía
    @Operation(summary = "Buscar un cliente por email: una lista con él, o vacía si no está")
    @GetMapping("/api/clientes")
    public List<ClienteVistaDTO> buscarPorEmail(@RequestParam(required = false) String email) {
        return clientes.buscarPorEmail(email).map(vistas::cliente).stream().toList();
    }

    @Operation(summary = "Registrar un cliente con clave; si ya compró sin cuenta, pide el código de una de sus entradas")
    @PostMapping("/api/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ClienteVistaDTO> registrar(@Valid @RequestBody PedidoClienteDTO pedido) {
        ClienteVistaDTO cliente = vistas.cliente(clientes.registrar(
                pedido.nombre(), pedido.email(), pedido.password(), pedido.codigoReserva()));
        return Creado.en(UriComponentsBuilder.fromPath("/api/clientes")
                .queryParam("email", cliente.email()).encode().toUriString(), cliente);
    }
}
