package ar.uade.cine.controller.usuarios;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.service.usuarios.GestorClientes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Ruta de /api/clientes: búsqueda por email vía GestorClientes (el alta es al comprar o con Google, R28).
@Tag(name = "Clientes", description = "La búsqueda de quien compra")
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
}
