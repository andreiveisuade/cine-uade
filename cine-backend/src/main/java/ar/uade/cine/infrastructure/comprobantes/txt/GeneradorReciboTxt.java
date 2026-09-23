package ar.uade.cine.infrastructure.comprobantes.txt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.infrastructure.comprobantes.GeneradorRecibo;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;

/**
 * Escribe el recibo en tickets/recibo-&lt;id de pago&gt;.txt, en la misma carpeta que el
 * ticket: los dos se le entregan a la misma persona en el mismo mostrador.
 *
 * <p>Se numera por el id del pago porque documenta el cobro, y así se lo encuentra desde
 * el arqueo. No repite película ni butacas —eso ya lo dice el ticket—: acá va la plata,
 * que al reservar todavía no se conocía.
 */
public class GeneradorReciboTxt extends ComprobanteTxt implements GeneradorRecibo {

    public GeneradorReciboTxt(Path directorio) {
        super(directorio);
    }

    @Override
    public void emitir(Pago pago, Reserva reserva) {
        List<String> lineas = new ArrayList<>(List.of(
                linea(),
                centrar("CINE UADE"),
                centrar("RECIBO DE CAJA #" + pago.getId()),
                linea(),
                campo("Reserva", "#" + reserva.getId() + " - " + reserva.getCodigo()),
                campo("Entradas", String.valueOf(reserva.getCantidadEntradas())),
                campo("Cobrado", fecha(pago.getFecha())),
                linea(),
                campo("Subtotal", "$ " + pago.getSubtotal())));

        // Solo si hubo descuento: un "Descuento: $ 0.00" invita a preguntar por qué.
        if (pago.getDescuento().esMayorQue(Dinero.CERO)) {
            lineas.add(campo("Descuento", "$ " + pago.getDescuento()));
        }

        lineas.addAll(List.of(
                campo("Total", "$ " + pago.getMonto()),
                campo("Pago", pago.getMedio().name()),
                linea(),
                centrar("Comprobante de pago en efectivo"),
                centrar("Conservar hasta el ingreso a la sala"),
                linea()));

        escribir("recibo-" + pago.getId() + ".txt", lineas, "el recibo del pago " + pago.getId());
    }
}
