package ar.uade.cine.controller;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.controller.vistas.VistasVentas;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.dto.ventas.ArqueoVistaDTO;
import ar.uade.cine.dto.ventas.CheckoutVistaDTO;
import ar.uade.cine.dto.ventas.PagoVistaDTO;
import ar.uade.cine.dto.ventas.PedidoCheckoutDTO;
import ar.uade.cine.dto.ventas.PedidoPagoDTO;
import ar.uade.cine.dto.ventas.TotalMedioDTO;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.service.informes.Arqueo;
import ar.uade.cine.service.informes.GestorCaja;
import ar.uade.cine.service.ventas.ConsultasReservas;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.RecursoNoEncontrado;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Cobros", description = "El cobro de una reserva y el arqueo de boletería")
@RestController
public class PagoController {

    private final GestorPagos pagos;
    private final ConsultasReservas reservas;
    private final GestorCaja caja;
    private final VistasVentas vistas;

    public PagoController(GestorPagos pagos, ConsultasReservas reservas, GestorCaja caja,
                            VistasVentas vistas) {
        this.pagos = pagos;
        this.reservas = reservas;
        this.caja = caja;
        this.vistas = vistas;
    }

    @Operation(summary = "Cobrar una reserva. El monto sale de la reserva, no del pedido")
    @PostMapping("/api/reservas/{id}/pago")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PagoVistaDTO> cobrar(@PathVariable int id, @Valid @RequestBody PedidoPagoDTO pedido) {
        exigirReserva(id);
        MedioPago medio = Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");
        return Creado.en("/api/reservas/" + id + "/pago",
                vistas.pago(pagos.cobrar(id, medio, pedido.codigoAutorizacion())));
    }

    @Operation(summary = "El pago de una reserva, o null si todavía no se cobró")
    @GetMapping(value = "/api/reservas/{id}/pago", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> pagoDe(@PathVariable int id) {
        exigirReserva(id);
        Optional<Pago> pago = pagos.buscarPorReserva(id);
        return ResponseEntity.ok(pago.map(p -> (Object) vistas.pago(p)).orElse("null"));
    }

    @Operation(summary = "Abrir el checkout electrónico: devuelve el QR y el link de pago")
    @PostMapping("/api/reservas/{id}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutVistaDTO abrirCheckout(@PathVariable int id,
                                          @Valid @RequestBody PedidoCheckoutDTO pedido) {
        exigirReserva(id);
        MedioPago medio = Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");

        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(id, medio);
        return new CheckoutVistaDTO(checkout.id(), checkout.reservaId(), checkout.medio().name(),
                checkout.monto().aPesos(), checkout.urlPago(), checkout.codigoQr());
    }

    @Operation(summary = "Confirmar el checkout una vez que el cliente pagó")
    @PostMapping("/api/checkouts/{id}/confirmacion")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PagoVistaDTO> confirmarCheckout(@PathVariable String id) {
        PagoVistaDTO pago = vistas.pago(pagos.confirmarCheckout(id));
        return Creado.en("/api/reservas/" + pago.reservaId() + "/pago", pago);
    }

    @Operation(summary = "El arqueo de boletería de un día")
    @GetMapping("/api/arqueo")
    public ArqueoVistaDTO arqueo(@RequestParam(required = false) String fecha) {
        Arqueo arqueo = caja.arqueoDe(Parseo.dia(fecha, "la fecha"));
        return new ArqueoVistaDTO(arqueo.fecha().toString(), arqueo.total().aPesos(),
                arqueo.entradas(), porMedio(arqueo),
                arqueo.pagos().stream().map(vistas::pagoDeArqueo).toList());
    }

    private void exigirReserva(int id) {
        reservas.buscar(id).orElseThrow(() -> new RecursoNoEncontrado("No existe la reserva " + id));
    }

    private static Map<String, TotalMedioDTO> porMedio(Arqueo arqueo) {
        Map<String, TotalMedioDTO> resumen = new TreeMap<>();
        arqueo.porMedio().forEach((medio, acumulado) ->
                resumen.put(medio.name(),
                        new TotalMedioDTO(acumulado.cantidad(), acumulado.total().aPesos())));
        return resumen;
    }
}
