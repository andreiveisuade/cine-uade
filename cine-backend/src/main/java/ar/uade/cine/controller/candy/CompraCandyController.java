package ar.uade.cine.controller.candy;

import java.security.Principal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.dto.candy.CompraCandyVistaDTO;
import ar.uade.cine.dto.candy.PedidoRetiroDTO;
import ar.uade.cine.dto.candy.PedidoVentaDTO;
import ar.uade.cine.service.candy.GestorCandy;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/candy/compras y /retiros: traduce HTTP a GestorCandy; separado de la carta porque no la edita.
// Comparte el tag con ProductoController para que Swagger muestre el candy en un solo grupo.
@Tag(name = "Candy", description = "La carta del candy y sus ventas de mostrador")
@RestController
@RequiredArgsConstructor
public class CompraCandyController {

    private final GestorCandy candy;
    private final VistasCandy vistas;

    // Alta sin Location: una compra no tiene GET por id, se la ve en el listado del día o del cliente.
    @Operation(summary = "Vender candy en el mostrador: nace cobrado")
    @PostMapping("/api/candy/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public CompraCandyVistaDTO vender(@Valid @RequestBody PedidoVentaDTO pedido) {
        MedioPago medio = Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");

        CompraCandy compra = pedido.reservaId() == null
                ? candy.vender(pedido.clienteId(), pedido.cantidades(), medio, pedido.codigoAutorizacion())
                : candy.venderParaReserva(pedido.reservaId(), pedido.clienteId(), pedido.cantidades(), medio,
                        pedido.codigoAutorizacion());

        return vistas.compra(compra);
    }

    @Operation(summary = "Las compras de candy de un día, o las de un cliente")
    @GetMapping("/api/candy/compras")
    public List<CompraCandyVistaDTO> compras(@RequestParam(required = false) String fecha,
                                             @RequestParam(required = false) String clienteId) {
        List<CompraCandy> compras = clienteId != null && !clienteId.isBlank()
                ? candy.listarComprasDe(Parseo.numeroOpcional(clienteId, "el id del cliente"))
                : candy.listarComprasDelDia(Parseo.dia(fecha, "la fecha"));
        return compras.stream().map(vistas::compra).toList();
    }

    // CU-16: el cliente sale de la sesión, no del pedido, como en /reservas/mias.
    @Operation(summary = "Las compras de candy cobradas del cliente de la sesión, la más nueva primero")
    @GetMapping("/api/candy/compras/mias")
    public List<CompraCandyVistaDTO> mias(Principal cliente) {
        return candy.listarCobradasDe(cliente.getName()).stream().map(vistas::compra).toList();
    }

    // D7: por código, como la puerta: es lo que trae el QR de la entrada.
    @Operation(summary = "Entregar el candy cobrado de una entrada, con su QR")
    @PostMapping("/api/candy/retiros")
    public List<CompraCandyVistaDTO> retirar(@Valid @RequestBody PedidoRetiroDTO pedido) {
        return candy.retirar(pedido.codigo()).stream().map(vistas::compra).toList();
    }
}
