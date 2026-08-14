package ar.uade.cine.api;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dto.ventas.ArqueoVistaDTO;
import ar.uade.cine.dto.ventas.CheckoutVistaDTO;
import ar.uade.cine.dto.ventas.PedidoCheckoutDTO;
import ar.uade.cine.dto.ventas.PedidoPagoDTO;
import ar.uade.cine.dto.ventas.TotalMedioDTO;
import ar.uade.cine.pasarelas.PasarelaPagos;
import ar.uade.cine.servicio.Arqueo;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorReservas;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

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
class RutasPagos {

    static void registrar(Javalin app, GestorPagos pagos, GestorReservas reservas, VistasVentas vistas) {

        app.post("/api/reservas/{id}/pago", ctx -> {
            int reservaId = Parseo.id(ctx);
            reservas.buscar(reservaId)
                    .orElseThrow(() -> new NoEncontrado("No existe la reserva " + reservaId));

            PedidoPagoDTO pedido = ctx.bodyAsClass(PedidoPagoDTO.class);
            // Sin medio, el null llega al gestor y es él quien avisa que falta (R11 se
            // valida ahí mismo, según lo que exija la constante).
            MedioPago medio = pedido.medio() == null
                    ? null
                    : Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");

            Pago pago = pagos.cobrar(reservaId, medio, pedido.codigoAutorizacion());
            ctx.status(HttpStatus.CREATED).json(vistas.pago(pago));
        });

        app.get("/api/reservas/{id}/pago", ctx -> {
            int reservaId = Parseo.id(ctx);
            reservas.buscar(reservaId)
                    .orElseThrow(() -> new NoEncontrado("No existe la reserva " + reservaId));

            Optional<Pago> pago = pagos.buscarPorReserva(reservaId);
            if (pago.isEmpty()) {
                // Una reserva sin cobrar no es un error: el front pregunta justamente
                // para saber si ya se pagó.
                ctx.contentType("application/json").result("null");
            } else {
                ctx.json(vistas.pago(pago.get()));
            }
        });

        // Abrir el checkout: lo que devuelve es el QR y el link que ve el cliente.
        app.post("/api/reservas/{id}/checkout", ctx -> {
            int reservaId = Parseo.id(ctx);
            reservas.buscar(reservaId)
                    .orElseThrow(() -> new NoEncontrado("No existe la reserva " + reservaId));

            PedidoCheckoutDTO pedido = ctx.bodyAsClass(PedidoCheckoutDTO.class);
            MedioPago medio = pedido.medio() == null
                    ? null
                    : Parseo.constante(MedioPago.class, pedido.medio(), "el medio de pago");

            PasarelaPagos.Checkout checkout = pagos.iniciarCheckout(reservaId, medio);
            ctx.status(HttpStatus.CREATED).json(new CheckoutVistaDTO(checkout.id(),
                    checkout.reservaId(), checkout.medio().name(), checkout.monto(),
                    checkout.urlPago(), checkout.codigoQr()));
        });

        // El id del checkout es del procesador y no un número nuestro, así que no pasa por
        // Parseo.id: viaja tal cual vino.
        app.post("/api/checkouts/{id}/confirmacion", ctx -> {
            Pago pago = pagos.confirmarCheckout(ctx.pathParam("id"));
            ctx.status(HttpStatus.CREATED).json(vistas.pago(pago));
        });

        app.get("/api/arqueo", ctx -> {
            Arqueo arqueo = pagos.arqueoDe(Parseo.dia(ctx.queryParam("fecha"), "la fecha"));
            ctx.json(new ArqueoVistaDTO(arqueo.fecha().toString(), arqueo.total(), arqueo.entradas(),
                    porMedio(arqueo), arqueo.pagos().stream().map(vistas::pagoDeArqueo).toList()));
        });
    }

    /**
     * El reparto por medio, con el nombre de la constante como clave. En TreeMap porque el
     * front lista los medios en el orden en que vienen y alfabético es un orden estable.
     */
    private static Map<String, TotalMedioDTO> porMedio(Arqueo arqueo) {
        Map<String, TotalMedioDTO> resumen = new TreeMap<>();
        arqueo.porMedio().forEach((medio, acumulado) ->
                resumen.put(medio.name(), new TotalMedioDTO(acumulado.cantidad(), acumulado.total())));
        return resumen;
    }
}
