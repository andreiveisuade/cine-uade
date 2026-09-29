package ar.uade.cine.controller.ventas;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.dto.ventas.PedidoAccesoDTO;
import ar.uade.cine.dto.ventas.ReservaVistaDTO;
import ar.uade.cine.service.ventas.GestorAcceso;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Ruta POST /api/acceso (Puerta): valida el código y registra el ingreso vía GestorAcceso; no decide nada.
// La Puerta: la usa el acomodador y no la boletería. Devuelve una reserva, pero cambia con el
// control de acceso y no con el circuito de compra.
@Tag(name = "Reservas")
@RestController
@RequiredArgsConstructor
public class AccesoController {

    private final GestorAcceso acceso;
    private final VistasReservas vistas;

    // Por código y no por id: el código es la única credencial del cliente y el id se adivina.
    @Operation(summary = "Validar el QR en la puerta y marcar la entrada como usada")
    @PostMapping("/api/acceso")
    public ReservaVistaDTO registrarIngreso(@Valid @RequestBody PedidoAccesoDTO pedido) {
        return vistas.reserva(acceso.registrarIngreso(pedido.codigo()));
    }
}
