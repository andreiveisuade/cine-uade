package ar.uade.cine.infrastructure.comprobantes.txt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.infrastructure.comprobantes.GeneradorRecibo;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;

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
