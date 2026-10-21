package ar.uade.cine.controller.ventas;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.dto.ventas.CheckoutVistaDTO;
import ar.uade.cine.dto.ventas.PagoVistaDTO;
import ar.uade.cine.dto.ventas.PedidoCheckoutDTO;
import ar.uade.cine.dto.ventas.PedidoPagoDTO;
import ar.uade.cine.service.ventas.ConsultasReservas;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.RetornoCheckout;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de pago y checkout de una reserva: traduce HTTP a GestorPagos; el monto no viaja en el pedido.
// El arqueo de boletería comparte el tag pero es de informes/CajaController: lee la caja, no cobra.
@Tag(name = "Cobros", description = "El cobro de una reserva y el arqueo de boletería")
@RestController
@RequiredArgsConstructor
public class PagoController {

    private final GestorPagos pagos;
    private final ConsultasReservas consultas;
    private final VistasPagos vistas;

    @Operation(summary = "Cobrar una reserva. El monto sale de la reserva, no del pedido")
    @PostMapping("/api/reservas/{id}/pago")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PagoVistaDTO> cobrar(@PathVariable int id, @Valid @RequestBody PedidoPagoDTO pedido) {
        MedioPago medio = Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");
        return Creado.en("/api/reservas/" + id + "/pago",
                vistas.pago(pagos.cobrar(id, medio, pedido.codigoAutorizacion())));
    }

    // Una reserva sin cobrar no tiene el recurso: 404, y el texto distingue eso de una reserva que no existe.
    @Operation(summary = "El pago de una reserva; 404 si todavía no se cobró")
    @GetMapping("/api/reservas/{id}/pago")
    public PagoVistaDTO pagoDe(@PathVariable int id) {
        return vistas.pago(pagos.buscarPorReserva(id)
                .orElseThrow(() -> new RecursoNoEncontrado("La reserva " + id + " todavía no tiene un pago")));
    }

    // Alta sin Location: el checkout vive en la pasarela y no tiene GET por id
    @Operation(summary = "Abrir el checkout electrónico: devuelve el QR y el link de pago")
    @PostMapping("/api/reservas/{id}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutVistaDTO abrirCheckout(@PathVariable int id,
                                          @Valid @RequestBody PedidoCheckoutDTO pedido) {
        MedioPago medio = Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");
        return vistas.checkout(pagos.iniciarCheckout(id, medio));
    }

    // El mismo checkout que el del encargado; la reserva sale del código y de la sesión (R21).
    @Operation(summary = "El cliente abre el checkout de su reserva, con la sesión abierta")
    @PostMapping("/api/reservas/codigo/{codigo}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutVistaDTO abrirCheckoutDelCliente(@PathVariable String codigo, Principal cliente,
                                                    @Valid @RequestBody PedidoCheckoutDTO pedido) {
        MedioPago medio = Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");
        int id = consultas.obtenerDelCliente(codigo, cliente.getName()).getId();
        return vistas.checkout(pagos.iniciarCheckout(id, medio));
    }

    @Operation(summary = "Confirmar el checkout una vez que el cliente pagó")
    @PostMapping("/api/checkouts/{id}/confirmacion")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PagoVistaDTO> confirmarCheckout(@PathVariable String id) {
        PagoVistaDTO pago = vistas.pago(pagos.confirmarCheckout(id));
        return Creado.en("/api/reservas/" + pago.reservaId() + "/pago", pago);
    }

    // back_url de Mercado Pago: llega el navegador del comprador, así que responde 302 al ticket (motivo en `aviso`)
    @Operation(summary = "Vuelta desde Mercado Pago: cobra si MP aprobó el pago y redirige al ticket")
    @GetMapping("/api/checkouts/{id}/retorno")
    public ResponseEntity<Void> retornoCheckout(@PathVariable String id) {
        RetornoCheckout retorno = pagos.retornarDeCheckout(id);
        String destino = "/#/ticket/" + retorno.codigoReserva() + (retorno.aviso() == null
                ? "" : "?aviso=" + URLEncoder.encode(retorno.aviso(), StandardCharsets.UTF_8));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(destino)).build();
    }
}
