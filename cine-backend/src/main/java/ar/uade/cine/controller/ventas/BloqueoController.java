package ar.uade.cine.controller.ventas;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.ventas.BloqueoVistaDTO;
import ar.uade.cine.dto.ventas.PedidoBloqueoDTO;
import ar.uade.cine.service.ventas.BloqueoDeButacas;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Ruta POST /api/funciones/{id}/bloqueos: toma butacas mientras el cliente elige; delega en BloqueoDeButacas.
// Cuelga de /api/funciones pero es el primer paso de la compra: en Swagger va con las reservas.
@Tag(name = "Reservas")
@RestController
@RequiredArgsConstructor
public class BloqueoController {

    private final BloqueoDeButacas bloqueo;
    private final VistasReservas vistas;

    @Operation(summary = "Tomar butacas mientras el cliente elige. Vencen solas")
    @PostMapping("/api/funciones/{id}/bloqueos")
    public BloqueoVistaDTO bloquear(@PathVariable int id, @Valid @RequestBody PedidoBloqueoDTO pedido) {
        return vistas.bloqueo(bloqueo.bloquear(id, pedido.butacas(), pedido.sesion()));
    }
}
