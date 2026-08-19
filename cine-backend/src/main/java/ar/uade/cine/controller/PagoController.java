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

import ar.uade.cine.controller.http.NoEncontrado;
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
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;

/**
 * Cobro de una reserva y arqueo del día.
 *
 * <p>El pedido de cobro no lleva monto a propósito: sale del total de la reserva, que a
 * su vez es lo que se congeló en cada entrada al reservar. Si el importe fuera un dato
 * de entrada, se podría cobrar $100 una reserva de $16.000.
 *
 * <p>El cobro electrónico son dos pedidos y no uno —abrir el checkout, confirmarlo— porque
 * en el medio pasa algo que no depende del cine: el cliente escanea y paga. Lo que devuelve
 * el segundo es un pago igual al del efectivo; la diferencia es de dónde salió el código de
 * autorización.
 */
@RestController
public class PagoController {

    private final GestorPagos pagos;
    private final GestorReservas reservas;
    private final GestorCaja caja;
    private final VistasVentas vistas;

    public PagoController(GestorPagos pagos, GestorReservas reservas, GestorCaja caja,
                            VistasVentas vistas) {
        this.pagos = pagos;
        this.reservas = reservas;
        this.caja = caja;
        this.vistas = vistas;
    }

    @PostMapping("/api/reservas/{id}/pago")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoVistaDTO cobrar(@PathVariable int id, @RequestBody PedidoPagoDTO pedido) {
        exigirReserva(id);
        // Sin medio, el null llega al gestor y es él quien avisa que falta (R11 se valida
        // ahí mismo, según lo que exija la constante).
        MedioPago medio = pedido.medio() == null
                ? null : Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");
        return vistas.pago(pagos.cobrar(id, medio, pedido.codigoAutorizacion()));
    }

    /**
     * Una reserva sin cobrar no es un error: el front pregunta justamente para saber si ya
     * se pagó, así que la respuesta es el literal {@code null}. Es el mismo caso que la
     * búsqueda de cliente por email, y por eso también se arma con {@link ResponseEntity}.
     */
    @GetMapping(value = "/api/reservas/{id}/pago", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> pagoDe(@PathVariable int id) {
        exigirReserva(id);
        Optional<Pago> pago = pagos.buscarPorReserva(id);
        return ResponseEntity.ok(pago.map(p -> (Object) vistas.pago(p)).orElse("null"));
    }

    /** Abrir el checkout: lo que devuelve es el QR y el link que ve el cliente. */
    @PostMapping("/api/reservas/{id}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutVistaDTO abrirCheckout(@PathVariable int id,
                                          @RequestBody PedidoCheckoutDTO pedido) {
        exigirReserva(id);
        MedioPago medio = pedido.medio() == null
                ? null : Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");

        PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(id, medio);
        return new CheckoutVistaDTO(checkout.id(), checkout.reservaId(), checkout.medio().name(),
                checkout.monto().aPesos(), checkout.urlPago(), checkout.codigoQr());
    }

    /**
     * El id del checkout es del procesador y no un número nuestro, así que viaja como
     * {@code String} y no como {@code int}: no es un id de los nuestros.
     */
    @PostMapping("/api/checkouts/{id}/confirmacion")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoVistaDTO confirmarCheckout(@PathVariable String id) {
        return vistas.pago(pagos.confirmarCheckout(id));
    }

    @GetMapping("/api/arqueo")
    public ArqueoVistaDTO arqueo(@RequestParam(required = false) String fecha) {
        Arqueo arqueo = caja.arqueoDe(Parseo.dia(fecha, "la fecha"));
        return new ArqueoVistaDTO(arqueo.fecha().toString(), arqueo.total().aPesos(),
                arqueo.entradas(), porMedio(arqueo),
                arqueo.pagos().stream().map(vistas::pagoDeArqueo).toList());
    }

    private void exigirReserva(int id) {
        reservas.buscar(id).orElseThrow(() -> new NoEncontrado("No existe la reserva " + id));
    }

    /**
     * El reparto por medio, con el nombre de la constante como clave. En TreeMap porque el
     * front lista los medios en el orden en que vienen y alfabético es un orden estable.
     */
    private static Map<String, TotalMedioDTO> porMedio(Arqueo arqueo) {
        Map<String, TotalMedioDTO> resumen = new TreeMap<>();
        arqueo.porMedio().forEach((medio, acumulado) ->
                resumen.put(medio.name(),
                        new TotalMedioDTO(acumulado.cantidad(), acumulado.total().aPesos())));
        return resumen;
    }
}
