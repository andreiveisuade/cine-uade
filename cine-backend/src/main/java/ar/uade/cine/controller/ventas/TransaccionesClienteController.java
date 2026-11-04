package ar.uade.cine.controller.ventas;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.candy.VistasCandy;
import ar.uade.cine.dto.ventas.TransaccionesClienteDTO;
import ar.uade.cine.service.candy.GestorCandy;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.ConsultasReservas;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Ruta de las transacciones de un cliente; acá y no en ClienteController porque compone reservas y candy.
@Tag(name = "Clientes")
@RestController
@RequiredArgsConstructor
public class TransaccionesClienteController {

    private final GestorClientes clientes;
    private final ConsultasReservas reservas;
    private final GestorCandy candy;
    private final VistasReservas vistasReservas;
    private final VistasCandy vistasCandy;

    @Operation(summary = "Las reservas y el candy suelto de un cliente, lo más nuevo primero (encargado)")
    @GetMapping("/api/clientes/{id}/transacciones")
    public TransaccionesClienteDTO deCliente(@PathVariable int id) {
        int clienteId = clientes.obtener(id).getId();
        return new TransaccionesClienteDTO(vistasReservas.reservas(reservas.listarPorCliente(clienteId)),
                candy.comprasSueltasDe(clienteId).stream().map(vistasCandy::compra).toList());
    }
}
